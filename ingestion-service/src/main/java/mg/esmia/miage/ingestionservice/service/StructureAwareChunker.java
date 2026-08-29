package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.CanonicalDocument;
import mg.esmia.miage.ingestionservice.dto.ast.DocumentElement;
import mg.esmia.miage.ingestionservice.dto.ast.ElementType;
import mg.esmia.miage.ingestionservice.dto.ast.PageAST;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Découpage orienté structure directement depuis le CanonicalDocument AST.
 *
 * <p>Contrairement au {@link MarkdownFallbackChunker} qui travaille sur du Markdown brut
 * pour les documents non-PDF, ce chunker consomme l'AST typé et préserve :
 * <ul>
 *   <li>la hiérarchie des titres ({@code headingPath}) ;</li>
 *   <li>l'atomicité des tableaux, code blocks et figures ;</li>
 *   <li>les plages de pages réelles ;</li>
 *   <li>les types d'éléments contenus dans chaque chunk.</li>
 * </ul>
 *
 * <p>Invariants :
 * <ul>
 *   <li>un heading est du <b>contexte</b>, pas du contenu — il met à jour le {@code headingPath}
 *       mais ne crée pas de chunk dédié ;</li>
 *   <li>un TABLE ou CODE est <b>atomique</b> — jamais cassé, même s'il dépasse la taille cible ;</li>
 *   <li>le préambule (contenu avant le premier heading) a {@code headingPath = []} ;</li>
 *   <li>l'ordre de lecture du parser PyMuPDF est conservé ;</li>
 *   <li>le merge des petits chunks ne fusionne que des chunks du même {@code headingPath}.</li>
 * </ul>
 */
@Service
public class StructureAwareChunker {

    /** Taille de chunk cible (chars). Un élément atomique peut dépasser cette limite. */
    private static final int MAX_CHUNK_CHARS = 1500;

    /** Taille minimale d'un chunk. En dessous, le chunk est fusionné avec le précédent
     *  (uniquement s'ils partagent le même headingPath). */
    private static final int MIN_CHUNK_CHARS = 100;

    /** Profondeur maximale de résolution parent_id (protection contre les cycles). */
    private static final int MAX_HEADING_DEPTH = 10;

    /**
     * Découpe un document AST en chunks structurés.
     *
     * @return la liste des chunks (non vide si le document contient du contenu)
     */
    public List<StructuredChunk> chunk(CanonicalDocument document) {
        if (document == null || document.pages() == null || document.pages().isEmpty()) {
            return List.of();
        }

        // 1. Aplatir les éléments dans l'ordre du parser (page par page)
        List<DocumentElement> allElements = flattenElements(document);

        // 2. Construire les paths des headings par parent_id
        Map<String, List<String>> headingPaths = buildHeadingPaths(allElements);

        // 3. Parcourir les éléments avec contexte courant
        List<StructuredChunk> rawChunks = buildChunks(allElements, headingPaths);

        // 4. Post-traitement : merge des petits chunks (même headingPath uniquement)
        return mergeTinyChunks(rawChunks);
    }

    /** Aplatit tous les éléments de toutes les pages dans l'ordre du parser. */
    private List<DocumentElement> flattenElements(CanonicalDocument document) {
        List<DocumentElement> elements = new ArrayList<>();
        for (PageAST page : document.pages()) {
            if (page.elements() != null) {
                elements.addAll(page.elements());
            }
        }
        return elements;
    }

    /**
     * Construit les chemins hiérarchiques de chaque heading en suivant parent_id.
     * Protection contre les cycles et les références inexistantes.
     */
    private Map<String, List<String>> buildHeadingPaths(List<DocumentElement> elements) {
        Map<String, DocumentElement> byId = new HashMap<>();
        for (DocumentElement e : elements) {
            if (e.type() == ElementType.HEADING && e.id() != null) {
                byId.put(e.id(), e);
            }
        }

        Map<String, List<String>> paths = new HashMap<>();
        for (DocumentElement e : elements) {
            if (e.type() != ElementType.HEADING || e.id() == null) {
                continue;
            }
            List<String> path = new ArrayList<>();
            DocumentElement current = e;
            int depth = 0;
            while (current != null && depth < MAX_HEADING_DEPTH) {
                path.add(0, current.text());
                if (current.parentId() != null) {
                    current = byId.get(current.parentId());
                } else {
                    current = null;
                }
                depth++;
            }
            paths.put(e.id(), path);
        }
        return paths;
    }

    /**
     * Parcourt les éléments et construit les chunks en maintenant le contexte courant.
     */
    private List<StructuredChunk> buildChunks(
            List<DocumentElement> allElements,
            Map<String, List<String>> headingPaths) {

        List<StructuredChunk> chunks = new ArrayList<>();
        List<String> currentHeadingPath = List.of();
        ChunkBuilder builder = new ChunkBuilder(currentHeadingPath);

        for (DocumentElement element : allElements) {
            if (element.type() == ElementType.HEADING) {
                // Fermer le chunk courant avant de changer de contexte
                if (!builder.isEmpty()) {
                    chunks.add(builder.build(chunks.size()));
                }
                // Mettre à jour le contexte (les headings ne sont PAS du contenu)
                List<String> newPath = headingPaths.getOrDefault(
                        element.id(), List.of(element.text()));
                builder = new ChunkBuilder(newPath);
                continue;
            }

            // Éléments atomiques : TABLE et CODE
            if (element.type() == ElementType.TABLE || element.type() == ElementType.CODE) {
                // Fermer le chunk courant s'il n'est pas vide
                if (!builder.isEmpty()) {
                    chunks.add(builder.build(chunks.size()));
                    builder = new ChunkBuilder(currentHeadingPath);
                }
                // Créer un chunk dédié pour l'élément atomique
                ChunkBuilder atomicBuilder = new ChunkBuilder(currentHeadingPath);
                atomicBuilder.addElement(element);
                chunks.add(atomicBuilder.build(chunks.size()));
                continue;
            }

            // Éléments normaux : PARAGRAPH, LIST, QUOTE, CAPTION, FIGURE
            String text = element.text();
            if (text == null || text.isBlank()) {
                continue;
            }

            // Si l'ajout dépasse la taille cible et le builder n'est pas vide, fermer
            if (!builder.isEmpty() && builder.length() + text.length() > MAX_CHUNK_CHARS) {
                chunks.add(builder.build(chunks.size()));
                builder = new ChunkBuilder(currentHeadingPath);
            }

            builder.addElement(element);
        }

        // Fermer le dernier chunk
        if (!builder.isEmpty()) {
            chunks.add(builder.build(chunks.size()));
        }

        return chunks;
    }

    /**
     * Fusionne les chunks trop petits avec le chunk précédent,
     * uniquement s'ils partagent le même headingPath.
     */
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
                // Fusionner dans le chunk précédent
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
                    i,
                    c.text(),
                    c.headingPath(),
                    c.pageStart(),
                    c.pageEnd(),
                    c.elementTypes(),
                    c.imageIds()));
        }
        return result;
    }

    /** Fusionne deux chunks en concaténant leurs textes et en unifiant leurs métadonnées. */
    private StructuredChunk mergeChunks(StructuredChunk first, StructuredChunk second) {
        String mergedText = first.text() + "\n\n" + second.text();
        Set<ElementType> mergedTypes = new HashSet<>(first.elementTypes());
        mergedTypes.addAll(second.elementTypes());
        List<String> mergedImageIds = new ArrayList<>(first.imageIds());
        mergedImageIds.addAll(second.imageIds());
        return new StructuredChunk(
                first.chunkIndex(),
                mergedText,
                first.headingPath(),
                Math.min(first.pageStart(), second.pageStart()),
                Math.max(first.pageEnd(), second.pageEnd()),
                Collections.unmodifiableSet(mergedTypes),
                Collections.unmodifiableList(mergedImageIds));
    }

    // -----------------------------------------------------------------------
    // ChunkBuilder — accumule les éléments d'un chunk en cours de construction
    // -----------------------------------------------------------------------

    private static class ChunkBuilder {
        private final List<String> headingPath;
        private final StringBuilder text = new StringBuilder();
        private int pageStart = Integer.MAX_VALUE;
        private int pageEnd = 0;
        private final Set<ElementType> elementTypes = new HashSet<>();
        private final List<String> imageIds = new ArrayList<>();

        ChunkBuilder(List<String> headingPath) {
            this.headingPath = headingPath;
        }

        void addElement(DocumentElement element) {
            String elementText = element.text();
            if (elementText != null && !elementText.isBlank()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append(elementText);
            }
            pageStart = Math.min(pageStart, element.page());
            pageEnd = Math.max(pageEnd, element.page());
            elementTypes.add(element.type());
            if (element.type() == ElementType.FIGURE && element.imageId() != null) {
                imageIds.add(element.imageId());
            }
        }

        boolean isEmpty() {
            return text.isEmpty();
        }

        int length() {
            return text.length();
        }

        StructuredChunk build(int index) {
            return new StructuredChunk(
                    index,
                    text.toString(),
                    headingPath,
                    pageStart == Integer.MAX_VALUE ? 0 : pageStart,
                    pageEnd,
                    Collections.unmodifiableSet(new HashSet<>(elementTypes)),
                    Collections.unmodifiableList(new ArrayList<>(imageIds)));
        }
    }
}
