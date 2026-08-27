package mg.esmia.miage.chatservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Représentation structurée d'une réponse LLM. Le parser découpe le contenu brut
 * en blocs ordonnés (Markdown, code, Mermaid, LaTeX, images) pour un rendu riche côté
 * frontend. Le champ {@code rawMarkdown} est conservé comme fallback.
 *
 * @param blocks       liste ordonnée de blocs extraits
 * @param rawMarkdown  contenu brut complet (fallback / compatibilité ascendante)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StructuredContent(
        List<ContentBlock> blocks,
        String rawMarkdown
) {

    /**
     * Un bloc individuel de la réponse.
     *
     * @param type     type du bloc
     * @param content  contenu textuel du bloc (code brut, markdown, etc.)
     * @param language langage du code block (ex. "java", "python"), nul sinon
     * @param url      URL de l'image, nul sinon
     * @param alt      texte alternatif de l'image, nul sinon
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContentBlock(
            BlockType type,
            String content,
            String language,
            String url,
            String alt
    ) {
        public ContentBlock(BlockType type, String content) {
            this(type, content, null, null, null);
        }

        public static ContentBlock markdown(String content) {
            return new ContentBlock(BlockType.MARKDOWN, content);
        }

        public static ContentBlock code(String language, String content) {
            return new ContentBlock(BlockType.CODE, content, language, null, null);
        }

        public static ContentBlock mermaid(String content) {
            return new ContentBlock(BlockType.MERMAID, content);
        }

        public static ContentBlock math(String content, boolean display) {
            return new ContentBlock(display ? BlockType.MATH_DISPLAY : BlockType.MATH_INLINE, content);
        }

        public static ContentBlock image(String url, String alt) {
            return new ContentBlock(BlockType.IMAGE, null, null, url, alt);
        }
    }

    public enum BlockType {
        MARKDOWN,
        CODE,
        MERMAID,
        MATH_INLINE,
        MATH_DISPLAY,
        IMAGE
    }
}
