package mg.esmia.miage.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.chatservice.dto.StructuredContent;
import mg.esmia.miage.chatservice.dto.StructuredContent.BlockType;
import mg.esmia.miage.chatservice.dto.StructuredContent.ContentBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse le contenu brut d'une réponse LLM en une liste ordonnée de blocs structurés.
 * La stratégie est de détecter les blocs « spéciaux » (code, mermaid, math, images)
 * via des regex, puis de traiter le reste comme du Markdown.
 *
 * <p>Sécurité : les images ne sont acceptées que si l'URL appartient à un domaine autorisé.
 * Les blocs de code ne sont jamais exécutés — ils sont rendus tels quels côté frontend.
 *
 * <p>Les regex sont appliquées sur le texte brut. Les blocs sont triés par position
 * dans le texte original pour conserver l'ordre d'apparition.
 */
@Component
@Slf4j
public class ResponseParser {

    /**
     * Domains dont les images sont acceptées. Les images provenant d'autres domaines
     * sont converties en bloc MARKDOWN (texte brut) pour éviter les requêtes réseau
     * arbitraires depuis le frontend.
     */
    private static final Set<String> ALLOWED_IMAGE_DOMAINS = Set.of(
            "localhost",
            "minio",
            "tsimoka-minio"
    );

    // --- Patterns ---

    // Fenced code blocks : ```lang\n...``` ou ```lang...``` (P1: \n optionnel après ouverture)
    // Le langage peut être vide ou absent. Backreference \1 pour matcher le même nombre de backticks.
    private static final Pattern FENCED_CODE = Pattern.compile(
            "(?m)(`{3,})(\\w*)[ \\t]*\\n?(.*?)\\1",
            Pattern.DOTALL
    );

    // LaTeX display : $$...$$ (sur plusieurs lignes, avec espaces possibles)
    private static final Pattern MATH_DISPLAY = Pattern.compile(
            "\\$\\$\\s*\\n?(.*?)\\n?\\s*\\$\\$",
            Pattern.DOTALL
    );

    // LaTeX inline : $...$ (pas de saut de ligne, pas de $$)
    // Lookbehind/lookahead pour éviter les $$ et les $ dans du code
    // P3:(?!\d) exclut $10, $20 etc. (prix/montants) pour éviter les faux positifs
    private static final Pattern MATH_INLINE = Pattern.compile(
            "(?<!\\$)\\$(?!\\$)(?!\\d)([^$\\n]+?)\\$(?!\\$)"
    );

    // Images : ![alt](url)
    private static final Pattern IMAGE = Pattern.compile(
            "!\\[([^\\]]*)\\]\\(([^)]+)\\)"
    );

    /**
     * Parse le contenu brut d'une réponse LLM en blocs structurés.
     *
     * @param rawContent contenu brut généré par le LLM
     * @return structured content avec les blocs ordonnés
     */
    public StructuredContent parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new StructuredContent(List.of(), "");
        }

        List<Span> spans = new ArrayList<>();

        // 1. Détecter les fenced code blocks en priorité (contiennent tout le reste)
        extractSpans(rawContent, FENCED_CODE, spans, match -> {
            String lang = match.group(2);
            String code = match.group(3).strip();
            if ("mermaid".equalsIgnoreCase(lang)) {
                return ContentBlock.mermaid(code);
            }
            String language = lang.isBlank() ? null : lang;
            return ContentBlock.code(language, code);
        });

        // 2. Détecter les math display $$...$$
        extractSpans(rawContent, MATH_DISPLAY, spans, match ->
                ContentBlock.math(match.group(1).strip(), true));

        // 3. Détecter les images (avant math inline pour éviter les faux positifs avec $)
        extractSpans(rawContent, IMAGE, spans, match -> {
            String alt = match.group(1);
            String url = match.group(2);
            if (isAllowedImageUrl(url)) {
                return ContentBlock.image(url, alt);
            }
            // Image non autorisée → markdown brut
            log.debug("Image rejetée (domaine non autorisé) : {}", url);
            return ContentBlock.markdown(match.group(0));
        });

        // 4. Détecter le math inline $...$
        extractSpans(rawContent, MATH_INLINE, spans, match ->
                ContentBlock.math(match.group(1).strip(), false));

        // 5. Trier par position dans le texte original
        spans.sort(Comparator.comparingInt(Span::start));

        // 6. Construire les blocs finaux : les spans speciaux + le markdown entre eux
        List<ContentBlock> blocks = buildBlocks(rawContent, spans);

        return new StructuredContent(blocks, rawContent);
    }

    // --- Internals ---

    /**
     * Extrait les spans correspondant au pattern et les ajoute à la liste.
     */
    private void extractSpans(String text, Pattern pattern, List<Span> spans,
                              SpanFactory factory) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            ContentBlock block = factory.create(matcher);
            spans.add(new Span(matcher.start(), matcher.end(), block));
        }
    }

    /**
     * Construit la liste de blocs finaux en insérant du markdown brut
     * entre les spans spéciaux. P6: ignore les spans chevauchants (start < cursor).
     */
    private List<ContentBlock> buildBlocks(String text, List<Span> spans) {
        List<ContentBlock> blocks = new ArrayList<>();
        int cursor = 0;

        for (Span span : spans) {
            // P6 : ignorer les spans déjà couverts par un bloc précédent
            if (span.start < cursor) {
                continue;
            }
            // Ajouter le markdown brut avant ce span
            if (span.start > cursor) {
                String between = text.substring(cursor, span.start).strip();
                if (!between.isBlank()) {
                    blocks.add(ContentBlock.markdown(between));
                }
            }
            // Ajouter le bloc spécial
            blocks.add(span.block);
            cursor = span.end;
        }

        // Ajouter le markdown brut restant après le dernier span
        if (cursor < text.length()) {
            String remaining = text.substring(cursor).strip();
            if (!remaining.isBlank()) {
                blocks.add(ContentBlock.markdown(remaining));
            }
        }

        // Si aucun bloc n'a été trouvé, retourner le texte complet comme markdown
        if (blocks.isEmpty()) {
            blocks.add(ContentBlock.markdown(text.strip()));
        }

        return blocks;
    }

    /**
     * Vérifie que l'URL de l'image appartient à un domaine autorisé.
     * Les images LLM-arbitraires sont rejetées pour des raisons de sécurité.
     */
    private boolean isAllowedImageUrl(String url) {
        if (url == null) {
            return false;
        }
        // URLs data: ou relatives (fichiers du worker) sont acceptées
        if (url.startsWith("data:") || url.startsWith("/") || url.startsWith("./")) {
            return true;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return true; // chemin relatif
            }
            return ALLOWED_IMAGE_DOMAINS.contains(host.toLowerCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private record Span(int start, int end, ContentBlock block) {}

    @FunctionalInterface
    private interface SpanFactory {
        ContentBlock create(Matcher matcher);
    }
}
