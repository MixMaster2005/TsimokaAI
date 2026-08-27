package mg.esmia.miage.chatservice.service;

import mg.esmia.miage.chatservice.dto.StructuredContent;
import mg.esmia.miage.chatservice.dto.StructuredContent.BlockType;
import mg.esmia.miage.chatservice.dto.StructuredContent.ContentBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseParserTest {

    private ResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ResponseParser();
    }

    // -----------------------------------------------------------------------
    // Cas simples
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("contenu null ou vide → bloc markdown vide")
    void nullOrBlank() {
        assertThat(parser.parse(null).blocks()).isEmpty();
        assertThat(parser.parse("").blocks()).isEmpty();
        assertThat(parser.parse("   ").blocks()).isEmpty();
    }

    @Test
    @DisplayName("texte pur sans bloc spécial → un seul bloc MARKDOWN")
    void plainMarkdown() {
        StructuredContent result = parser.parse("Bonjour, ceci est une réponse.");
        assertThat(result.blocks()).hasSize(1);
        assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
        assertThat(result.blocks().get(0).content()).isEqualTo("Bonjour, ceci est une réponse.");
        assertThat(result.rawMarkdown()).isEqualTo("Bonjour, ceci est une réponse.");
    }

    @Test
    @DisplayName("titres et formatage markdown préservés")
    void markdownFormatting() {
        String input = "# Titre\n\n**Gras** et *italique*.\n\n- item 1\n- item 2";
        StructuredContent result = parser.parse(input);
        assertThat(result.blocks()).hasSize(1);
        assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
        assertThat(result.blocks().get(0).content()).contains("# Titre");
        assertThat(result.blocks().get(0).content()).contains("**Gras**");
    }

    // -----------------------------------------------------------------------
    // Code blocks
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Code blocks")
    class CodeBlocks {

        @Test
        @DisplayName("code block avec langage")
        void codeWithLanguage() {
            String input = "Voici du code :\n\n```java\npublic class Example {}\n```\n\nFin.";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(3);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
            assertThat(result.blocks().get(0).content()).contains("Voici du code");

            assertThat(result.blocks().get(1).type()).isEqualTo(BlockType.CODE);
            assertThat(result.blocks().get(1).language()).isEqualTo("java");
            assertThat(result.blocks().get(1).content()).contains("public class Example {}");

            assertThat(result.blocks().get(2).type()).isEqualTo(BlockType.MARKDOWN);
            assertThat(result.blocks().get(2).content()).contains("Fin.");
        }

        @Test
        @DisplayName("code block sans langage")
        void codeWithoutLanguage() {
            String input = "```\nsome code\n```";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.CODE);
            assertThat(result.blocks().get(0).language()).isNull();
            assertThat(result.blocks().get(0).content()).isEqualTo("some code");
        }

        @Test
        @DisplayName("code block python")
        void codePython() {
            String input = "```python\nprint('hello')\n```";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.CODE);
            assertThat(result.blocks().get(0).language()).isEqualTo("python");
        }
    }

    // -----------------------------------------------------------------------
    // Mermaid
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Mermaid diagrams")
    class MermaidBlocks {

        @Test
        @DisplayName("mermaid block détecté comme bloc MERMAID")
        void mermaidDetected() {
            String input = "Voici un diagramme :\n\n```mermaid\nflowchart TD\n    A --> B\n```\n\nFin.";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(3);
            assertThat(result.blocks().get(1).type()).isEqualTo(BlockType.MERMAID);
            assertThat(result.blocks().get(1).content()).contains("flowchart TD");
            assertThat(result.blocks().get(1).content()).contains("A --> B");
        }

        @Test
        @DisplayName("mermaid sequence diagram")
        void mermaidSequence() {
            String input = "```mermaid\nsequenceDiagram\n    A->>B: hello\n```";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MERMAID);
        }
    }

    // -----------------------------------------------------------------------
    // LaTeX
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("LaTeX math")
    class MathBlocks {

        @Test
        @DisplayName("math inline $...$")
        void mathInline() {
            String input = "La formule $x^2 + y^2 = r^2$ est célèbre.";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(3);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
            assertThat(result.blocks().get(1).type()).isEqualTo(BlockType.MATH_INLINE);
            assertThat(result.blocks().get(1).content()).isEqualTo("x^2 + y^2 = r^2");
            assertThat(result.blocks().get(2).type()).isEqualTo(BlockType.MARKDOWN);
        }

        @Test
        @DisplayName("math display $$...$$")
        void mathDisplay() {
            String input = "On a :\n\n$$\n\\int_0^1 x\\,dx = \\frac{1}{2}\n$$\n\nDonc.";
            StructuredContent result = parser.parse(input);

            List<ContentBlock> mathBlocks = result.blocks().stream()
                    .filter(b -> b.type() == BlockType.MATH_DISPLAY)
                    .toList();
            assertThat(mathBlocks).hasSize(1);
            assertThat(mathBlocks.get(0).content()).contains("\\int_0^1");
        }

        @Test
        @DisplayName("math inline et display dans la même réponse")
        void mathMixed() {
            String input = "Inline $E = mc^2$ et display :\n\n$$\n\\sum_{i=1}^n i = \\frac{n(n+1)}{2}\n$$";
            StructuredContent result = parser.parse(input);

            long inlineCount = result.blocks().stream()
                    .filter(b -> b.type() == BlockType.MATH_INLINE).count();
            long displayCount = result.blocks().stream()
                    .filter(b -> b.type() == BlockType.MATH_DISPLAY).count();
            assertThat(inlineCount).isEqualTo(1);
            assertThat(displayCount).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Images
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Images")
    class ImageBlocks {

        @Test
        @DisplayName("image avec URL autorisée (localhost)")
        void allowedImage() {
            String input = "![schema](http://localhost:9000/bucket/img.png)";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.IMAGE);
            assertThat(result.blocks().get(0).url()).contains("localhost:9000");
            assertThat(result.blocks().get(0).alt()).isEqualTo("schema");
        }

        @Test
        @DisplayName("image avec URL non autorisée → markdown brut")
        void rejectedImage() {
            String input = "![schema](https://evil.com/hack.png)";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
            assertThat(result.blocks().get(0).content()).contains("![schema]");
        }

        @Test
        @DisplayName("image data: URI acceptée")
        void dataUriImage() {
            String input = "![img](data:image/png;base64,abc123)";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.IMAGE);
        }
    }

    // -----------------------------------------------------------------------
    // Mélange de types
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Mélange de types")
    class MixedContent {

        @Test
        @DisplayName("réponse complète avec markdown + code + mermaid + math")
        void fullResponse() {
            String input = """
                    # Introduction

                    Voici une formule : $E = mc^2$.

                    ```mermaid
                    flowchart LR
                        A --> B
                    ```

                    Et du code :

                    ```java
                    public class Main {
                        public static void main(String[] args) {
                            System.out.println("Hello");
                        }
                    }
                    ```

                    Le résultat est $\\frac{1}{2}$.
                    """;
            StructuredContent result = parser.parse(input);

            // Au minimum : markdown, math_inline, mermaid, code, math_inline
            assertThat(result.blocks().size()).isGreaterThanOrEqualTo(5);

            assertThat(result.blocks()).anyMatch(b -> b.type() == BlockType.MARKDOWN);
            assertThat(result.blocks()).anyMatch(b -> b.type() == BlockType.CODE);
            assertThat(result.blocks()).anyMatch(b -> b.type() == BlockType.MERMAID);
            assertThat(result.blocks()).anyMatch(b -> b.type() == BlockType.MATH_INLINE);
        }

        @Test
        @DisplayName("ordre des blocs préservé")
        void blockOrderPreserved() {
            String input = "Avant\n\n```java\ncode\n```\n\nMilieu\n\n$ x $\n\nAprès";
            StructuredContent result = parser.parse(input);

            List<BlockType> types = result.blocks().stream().map(ContentBlock::type).toList();
            assertThat(types).containsExactly(
                    BlockType.MARKDOWN,  // "Avant"
                    BlockType.CODE,      // java code
                    BlockType.MARKDOWN,  // "Milieu"
                    BlockType.MATH_INLINE, // $ x $
                    BlockType.MARKDOWN   // "Après"
            );
        }
    }

    // -----------------------------------------------------------------------
    // Contenu malformé
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Contenu malformé")
    class MalformedContent {

        @Test
        @DisplayName("code block non fermé → traité comme markdown")
        void unclosedCodeBlock() {
            String input = "```\nunclosed code";
            StructuredContent result = parser.parse(input);

            // Le regex ne match pas, tout est markdown
            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
        }

        @Test
        @DisplayName("seul $ sans contenu → pas de math")
        void loneDollar() {
            String input = "Prix : 10 $ USD";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
        }

        @Test
        @DisplayName("$$ non fermé → pas de math display")
        void unclosedMathDisplay() {
            String input = "Formule :\n\n$$\n\\int x";
            StructuredContent result = parser.parse(input);

            assertThat(result.blocks()).hasSize(1);
            assertThat(result.blocks().get(0).type()).isEqualTo(BlockType.MARKDOWN);
        }
    }
}
