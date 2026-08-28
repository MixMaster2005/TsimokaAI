package mg.esmia.miage.ingestionservice.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Découpage du Markdown extrait en chunks **orienté sens** (spec v2 du chunking) :
 * <ol>
 *   <li>le document est découpé en sections délimitées par les titres Markdown
 *       ({@code #}, {@code ##}, …) — chaque section conserve son titre avec son contenu ;</li>
 *   <li>une section qui tient dans la taille cible devient un chunk tel quel ;</li>
 *   <li>une section trop grande est re-découpée **récursivement** sur le niveau de titre
 *       inférieur suivant ; ce n'est qu'en dernier recours (plus aucun sous-titre) qu'on
 *       retombe sur un découpage de taille fixe avec chevauchement, sur un séparateur
 *       d'espace pour ne jamais couper un mot.</li>
 * </ol>
 *
 * <p>Le sens est ainsi préservé : les titres ne sont jamais séparés de leur contenu et la
 * découpe finit par suivre la structure du document plutôt qu'une longueur brute.
 *
 * <p>Limites connues : un titre à l'intérieur d'un bloc de code peut provoquer une fausse
 * frontière de section, et un document très fragmenté peut produire des chunks très petits
 * (une section par chunk) — à observer sur le test de bout en bout.
 */
@Service
public class MarkdownChunkingService {

    /** Taille de chunk cible (tokens) — heuristique à ajuster empiriquement. */
    private static final int CHUNK_SIZE_TOKENS = 500;
    /** Chevauchement entre chunks consécutifs (tokens) — utilisé uniquement par la découpe
     *  de secours des sections surdimensionnées (un titre ne se chevauche jamais). */
    private static final int CHUNK_OVERLAP_TOKENS = 50;
    /** Heuristique simpliste tokens ≈ caractères / 3 (pas de tokenizer dédié).
     *  3 chars/token est plus réaliste pour le français (vs. 4 pour l'anglais). */
    private static final int CHARS_PER_TOKEN = 3;

    private static final int MAX_CHUNK_CHARS = CHUNK_SIZE_TOKENS * CHARS_PER_TOKEN;
    private static final int OVERLAP_CHARS = CHUNK_OVERLAP_TOKENS * CHARS_PER_TOKEN;
    /** Taille minimale d'un chunk (chars). Un chunk plus petit est fusionné avec le précédent. */
    private static final int MIN_CHUNK_CHARS = 100;
    private static final int MAX_HEADING_LEVEL = 6;

    /** Ligne de titre Markdown : 1 à 6 {@code #} suivis d'un espace/tabulation. */
    private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})[ \\t].*$");

    /** Bloc fenced code : protège les titres à l'intérieur des blocs de code du scan. */
    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```.*?```");

    /** Section du Markdown issue d'un découpage : texte + drapeau « commence par un titre
     *  au niveau {@code childLevel} » (vs. préambule appartenant au titre parent). */
    private record SectionPart(String text, boolean child) {
    }

    /**
     * Découpe un document Markdown en chunks orientés sens.
     *
     * @return la liste des chunks (texte non vide), vide si le Markdown est vide/blanc.
     */
    public List<String> chunk(String markdown) {
        List<String> chunks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return chunks;
        }
        chunkSection(stripCodeBlocks(normalize(markdown)), 0, chunks);
        return mergeTinyChunks(chunks);
    }

    /**
     * Remplace le contenu des blocs fenced code par des espaces de même longueur
     * pour éviter les faux titres (C3). Les blocs de code sont rétablis dans le
     * chunk final : seul le scan de titres est affecté, pas le contenu émis.
     */
    String stripCodeBlocks(String markdown) {
        Matcher m = FENCED_CODE.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replaced = " ".repeat(m.group(0).length());
            m.appendReplacement(sb, replaced);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Fusionne les chunks trop petits avec le chunk précédent pour éviter les
     * embeddings vides ou trop peu informatifs (C4).
     */
    private List<String> mergeTinyChunks(List<String> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }
        List<String> merged = new ArrayList<>();
        StringBuilder pending = new StringBuilder(chunks.get(0));
        for (int i = 1; i < chunks.size(); i++) {
            String current = chunks.get(i);
            if (pending.length() < MIN_CHUNK_CHARS) {
                pending.append("\n\n").append(current);
            } else {
                merged.add(pending.toString());
                pending = new StringBuilder(current);
            }
        }
        merged.add(pending.toString());
        return merged;
    }

    /** Estimation grossière du nombre de tokens d'un texte (chars / 4, minimum 1). */
    public int estimateTokenCount(String text) {
        return text == null ? 0 : Math.max(1, text.length() / CHARS_PER_TOKEN);
    }

    /** Nettoyage minimal des artefacts de conversion avant découpage et embedding. */
    String normalize(String markdown) {
        return markdown
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\f", "\n\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Traite une section (déjà délimitée par un titre de niveau {@code level}, ou le document
     * entier pour {@code level == 0}). La frontière première des chunks est la structure des
     * titres : dès qu'un sous-titre existe, on découpe sur lui. La taille n'entre en jeu que
     * pour une section sans sous-titre : trop grande → découpe fixe de secours.
     */
    private void chunkSection(String section, int level, List<String> out) {
        String trimmed = section.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        int childLevel = smallestHeadingLevelGreaterThan(trimmed, level);
        if (childLevel != -1) {
            for (SectionPart part : splitByLevel(trimmed, childLevel)) {
                chunkSection(part.text(), part.child() ? childLevel : level, out);
            }
            return;
        }
        if (trimmed.length() <= MAX_CHUNK_CHARS) {
            out.add(trimmed);
        } else {
            splitFixed(trimmed, out);
        }
    }

    /** Niveau de titre le plus petit présent dans {@code section}, strictement supérieur à
     *  {@code level} ; {@code -1} s'il n'y en a pas (section sans sous-titres). */
    private int smallestHeadingLevelGreaterThan(String section, int level) {
        int min = MAX_HEADING_LEVEL + 1;
        Matcher matcher = HEADING.matcher(section);
        while (matcher.find()) {
            int headingLevel = matcher.group(1).length();
            if (headingLevel > level && headingLevel < min) {
                min = headingLevel;
            }
        }
        return min == MAX_HEADING_LEVEL + 1 ? -1 : min;
    }

    /**
     * Découpe {@code section} aux lignes de titre de niveau exact {@code level}. Chaque
     * sous-section garde son titre ; le préambule avant le premier titre de ce niveau reste
     * rattaché au titre parent ({@code child == false}).
     */
    private List<SectionPart> splitByLevel(String section, int level) {
        List<SectionPart> parts = new ArrayList<>();
        Pattern childPattern = Pattern.compile("(?m)^(" + "#".repeat(level) + ")[ \\t]");
        Matcher matcher = childPattern.matcher(section);
        int pos = 0;
        while (matcher.find()) {
            if (matcher.start() > pos) {
                parts.add(new SectionPart(section.substring(pos, matcher.start()), false));
            }
            pos = matcher.start();
        }
        if (pos < section.length()) {
            parts.add(new SectionPart(section.substring(pos), true));
        }
        if (parts.isEmpty()) {
            parts.add(new SectionPart(section, false));
        }
        return parts;
    }

    /** Découpe de secours : taille fixe avec chevauchement, sans jamais couper un mot. */
    private void splitFixed(String text, List<String> out) {
        int length = text.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + MAX_CHUNK_CHARS, length);
            if (end < length) {
                end = chooseBoundary(text, start, end);
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                out.add(chunk);
            }
            if (end >= length) {
                break;
            }
            start = end - OVERLAP_CHARS;
        }
    }

    private int chooseBoundary(String text, int start, int end) {
        int lowerBound = start + MAX_CHUNK_CHARS / 2;

        int paragraph = text.lastIndexOf("\n\n", end);
        if (paragraph > lowerBound) {
            return paragraph + 2;
        }

        int sentence = Math.max(
                Math.max(text.lastIndexOf(". ", end), text.lastIndexOf("? ", end)),
                text.lastIndexOf("! ", end));
        if (sentence > lowerBound) {
            return sentence + 1;
        }

        int line = text.lastIndexOf('\n', end);
        if (line > lowerBound) {
            return line + 1;
        }

        int space = text.lastIndexOf(' ', end);
        if (space > lowerBound) {
            return space;
        }

        return end;
    }
}
