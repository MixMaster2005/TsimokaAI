package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.ElementType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chunking de fallback pour les documents non-PDF (DOCX, PPTX, HTML, etc.).
 *
 * <p>Quand le docling-worker ne produit pas d'AST ({@code document == null}),
 * ce chunker découpe le Markdown brut en {@link StructuredChunk} en suivant
 * la structure des titres ({@code #}, {@code ##}, …).</p>
 *
 * <p>Les invariants miment le {@link StructureAwareChunker} :
 * <ul>
 *   <li>un titre met à jour le {@code headingPath} mais ne crée pas de chunk dédié ;</li>
 *   <li>les blocs fenced code sont atomiques (jamais cassés) ;</li>
 *   <li>le préambule (avant le premier titre) a {@code headingPath = []} ;</li>
 *   <li>le merge des petits chunks ne fusionne que les chunks du même {@code headingPath}.</li>
 * </ul>
 *
 * <p>Pour les formats non-PDF, {@code pageStart} et {@code pageEnd} sont fixés à 1
 * (pas d'information de page fiable disponible).</p>
 */
@Service
public class MarkdownFallbackChunker {

    private static final int MAX_CHUNK_CHARS = 1500;
    private static final int MIN_CHUNK_CHARS = 100;
    private static final int MAX_HEADING_LEVEL = 6;

    private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})[ \\t](.*)$");
    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```.*?```");

    public List<StructuredChunk> chunk(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        String normalized = normalize(markdown);
        List<Block> blocks = parseBlocks(normalized);
        List<StructuredChunk> rawChunks = buildChunks(blocks);
        return mergeTinyChunks(rawChunks);
    }

    private String normalize(String markdown) {
        return markdown
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\f", "\n\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Parse le markdown en une liste ordonnée de blocs (titres, paragraphes, code blocks).
     * Les titres ne sont pas des chunks — ils servent uniquement de contexte.
     * Les titres à l'intérieur de blocs de code fenced sont ignorés.
     */
    private List<Block> parseBlocks(String markdown) {
        List<Block> blocks = new ArrayList<>();

        // 1. Trouver les plages de blocs de code fenced (protégées du scan de titres)
        List<Range> codeRanges = new ArrayList<>();
        Matcher cm = FENCED_CODE.matcher(markdown);
        while (cm.find()) {
            codeRanges.add(new Range(cm.start(), cm.end()));
        }

        // 2. Trouver les titres HORS des blocs de code
        Matcher hm = HEADING.matcher(markdown);
        List<MatchInfo> headings = new ArrayList<>();
        while (hm.find()) {
            int matchStart = hm.start();
            boolean insideCode = codeRanges.stream().anyMatch(r -> matchStart >= r.start() && matchStart < r.end());
            if (!insideCode) {
                headings.add(new MatchInfo(hm.start(), hm.end(),
                        hm.group(1).length(), hm.group(2).trim()));
            }
        }

        // 3. Découper le markdown en segments (avant/après chaque titre)
        int pos = 0;
        for (MatchInfo h : headings) {
            // Contenu avant ce titre (paragraphes)
            if (h.start() > pos) {
                String text = markdown.substring(pos, h.start()).trim();
                if (!text.isEmpty()) {
                    blocks.addAll(splitIntoParagraphs(text));
                }
            }
            blocks.add(new Block(BlockType.HEADING, h.level, h.text, List.of()));
            pos = h.end();
        }

        // Contenu après le dernier titre (ou le document entier si pas de titres)
        if (pos < markdown.length()) {
            String text = markdown.substring(pos).trim();
            if (!text.isEmpty()) {
                blocks.addAll(splitIntoParagraphs(text));
            }
        }

        return blocks;
    }

    /**
     * Découpe un texte en paragraphes, en détectant les blocs de code fenced.
     */
    private List<Block> splitIntoParagraphs(String text) {
        List<Block> blocks = new ArrayList<>();
        Matcher cm = FENCED_CODE.matcher(text);
        int pos = 0;
        List<Range> codeRanges = new ArrayList<>();
        while (cm.find()) {
            codeRanges.add(new Range(cm.start(), cm.end()));
        }

        for (Range codeRange : codeRanges) {
            // Texte avant le bloc de code
            if (codeRange.start > pos) {
                String before = text.substring(pos, codeRange.start).trim();
                addParagraphBlocks(before, blocks);
            }
            // Bloc de code (atomique)
            String codeContent = text.substring(codeRange.start, codeRange.end).trim();
            if (!codeContent.isEmpty()) {
                blocks.add(new Block(BlockType.CODE, 0, codeContent, List.of(ElementType.CODE)));
            }
            pos = codeRange.end;
        }
        // Texte après le dernier bloc de code (ou le texte entier si pas de code)
        if (pos < text.length()) {
            String after = text.substring(pos).trim();
            addParagraphBlocks(after, blocks);
        }
        return blocks;
    }

    private void addParagraphBlocks(String text, List<Block> blocks) {
        if (text.isEmpty()) return;
        // Séparer par double saut de ligne (paragraphes Markdown)
        String[] paragraphs = text.split("\\n\\n+", -1);
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(new Block(BlockType.PARAGRAPH, 0, trimmed, List.of(ElementType.PARAGRAPH)));
            }
        }
    }

    /**
     * Construit les chunks à partir des blocs parsés, en maintenant le headingPath courant.
     */
    private List<StructuredChunk> buildChunks(List<Block> blocks) {
        List<StructuredChunk> chunks = new ArrayList<>();
        List<String> currentHeadingPath = List.of();
        StringBuilder currentText = new StringBuilder();
        Set<ElementType> currentTypes = new HashSet<>();
        int chunkIndex = 0;

        for (Block block : blocks) {
            if (block.type == BlockType.HEADING) {
                // Fermer le chunk courant avant de changer de contexte
                if (!currentText.isEmpty()) {
                    chunks.add(buildChunk(chunkIndex++, currentText, currentHeadingPath, currentTypes));
                    currentText = new StringBuilder();
                    currentTypes = new HashSet<>();
                }
                // Mettre à jour le headingPath
                currentHeadingPath = buildHeadingPath(block.level, block.text, currentHeadingPath);
                continue;
            }

            if (block.type == BlockType.CODE) {
                // CODE est atomique : fermer le chunk courant, créer un chunk dédié
                if (!currentText.isEmpty()) {
                    chunks.add(buildChunk(chunkIndex++, currentText, currentHeadingPath, currentTypes));
                    currentText = new StringBuilder();
                    currentTypes = new HashSet<>();
                }
                chunks.add(buildChunk(chunkIndex++, new StringBuilder(block.text),
                        currentHeadingPath, Set.of(ElementType.CODE)));
                continue;
            }

            // PARAGRAPH : ajouter au chunk courant
            if (!currentText.isEmpty()) {
                currentText.append("\n\n");
            }
            currentText.append(block.text);
            currentTypes.addAll(block.elementTypes);

            // Si le chunk dépasse la taille cible, le fermer
            if (currentText.length() > MAX_CHUNK_CHARS) {
                chunks.add(buildChunk(chunkIndex++, currentText, currentHeadingPath, currentTypes));
                currentText = new StringBuilder();
                currentTypes = new HashSet<>();
            }
        }

        // Fermer le dernier chunk
        if (!currentText.isEmpty()) {
            chunks.add(buildChunk(chunkIndex, currentText, currentHeadingPath, currentTypes));
        }

        return chunks;
    }

    /**
     * Construit le headingPath en fonction du niveau du titre.
     * Un titre de niveau N écrase les niveaux >= N dans le path courant.
     */
    private List<String> buildHeadingPath(int level, String text, List<String> currentPath) {
        List<String> newPath = new ArrayList<>(currentPath);
        // Écraser les niveaux >= level
        while (!newPath.isEmpty() && newPath.size() >= level) {
            newPath.remove(newPath.size() - 1);
        }
        newPath.add(text);
        return Collections.unmodifiableList(newPath);
    }

    private StructuredChunk buildChunk(int index, StringBuilder text, List<String> headingPath,
                                        Set<ElementType> elementTypes) {
        return new StructuredChunk(
                index,
                embeddableText(text, headingPath),
                headingPath,
                1, 1, // pageStart/pageEnd : pas d'info de page pour non-PDF
                Collections.unmodifiableSet(new HashSet<>(elementTypes)),
                List.of());
    }

    /**
     * Texte final embedable : on préfixe le contenu par le chemin des titres
     * (headingPath) pour que les titres/sections soient réellement recherchables
     * par similarité vectorielle — un titre n'étant jamais stocké comme contenu,
     * sans ce préfixe une question portant sur le libellé d'un titre ne matcherait
     * pas le chunk via cosine similarity.
     */
    private String embeddableText(StringBuilder text, List<String> headingPath) {
        String content = text.toString().trim();
        if (content.isEmpty() || headingPath == null || headingPath.isEmpty()) {
            return content;
        }
        String prefix = String.join(" > ", headingPath);
        return prefix + "\n\n" + content;
    }

    private List<StructuredChunk> mergeTinyChunks(List<StructuredChunk> rawChunks) {
        if (rawChunks.size() <= 1) {
            return rawChunks;
        }

        List<StructuredChunk> merged = new ArrayList<>();
        StructuredChunk pending = rawChunks.get(0);

        for (int i = 1; i < rawChunks.size(); i++) {
            StructuredChunk current = rawChunks.get(i);
            if (pending.text().length() < MIN_CHUNK_CHARS
                    && pending.headingPath().equals(current.headingPath())) {
                pending = mergeChunks(pending, current);
            } else {
                merged.add(pending);
                pending = current;
            }
        }
        merged.add(pending);

        // Réindexer
        List<StructuredChunk> result = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            StructuredChunk c = merged.get(i);
            result.add(new StructuredChunk(
                    i, c.text(), c.headingPath(),
                    c.pageStart(), c.pageEnd(),
                    c.elementTypes(), c.imageIds()));
        }
        return result;
    }

    private StructuredChunk mergeChunks(StructuredChunk first, StructuredChunk second) {
        String mergedText = first.text() + "\n\n" + second.text();
        Set<ElementType> mergedTypes = new HashSet<>(first.elementTypes());
        mergedTypes.addAll(second.elementTypes());
        return new StructuredChunk(
                first.chunkIndex(), mergedText, first.headingPath(),
                first.pageStart(), first.pageEnd(),
                Collections.unmodifiableSet(mergedTypes), List.of());
    }

    // -----------------------------------------------------------------------
    // Types internes
    // -----------------------------------------------------------------------

    private enum BlockType { HEADING, PARAGRAPH, CODE }

    private static class Block {
        final BlockType type;
        final int level;       // niveau du titre (1-6), 0 sinon
        final String text;
        final List<ElementType> elementTypes;

        Block(BlockType type, int level, String text, List<ElementType> elementTypes) {
            this.type = type;
            this.level = level;
            this.text = text;
            this.elementTypes = elementTypes;
        }
    }

    private record MatchInfo(int start, int end, int level, String text) {}

    private record Range(int start, int end) {}
}
