import type { ContentBlock } from '../types';
import { CodeBlock } from './CodeBlock';
import { MermaidBlock } from './MermaidBlock';
import { MathBlock } from './MathBlock';

interface MessageRendererProps {
  blocks: ContentBlock[];
}

/**
 * Rend une liste de blocs structurés parsés côté backend.
 * Les blocs MARKDOWN sont rendus en texte brut pour l'instant
 * (pas de dépendance markdown-to-react nécessaire pour le MVP).
 */
export function MessageRenderer({ blocks }: MessageRendererProps) {
  return (
    <div className="space-y-1">
      {blocks.map((block, i) => (
        <BlockRenderer key={i} block={block} />
      ))}
    </div>
  );
}

function BlockRenderer({ block }: { block: ContentBlock }) {
  switch (block.type) {
    case 'CODE':
      return <CodeBlock language={block.language}>{block.content ?? ''}</CodeBlock>;

    case 'MERMAID':
      return <MermaidBlock chart={block.content ?? ''} />;

    case 'MATH_INLINE':
      return <MathBlock math={block.content ?? ''} display={false} />;

    case 'MATH_DISPLAY':
      return <MathBlock math={block.content ?? ''} display={true} />;

    case 'IMAGE':
      return (
        <figure className="my-3">
          <img
            src={block.url ?? ''}
            alt={block.alt ?? ''}
            className="max-w-full rounded-md border border-border"
          />
          {block.alt && (
            <figcaption className="mt-1 text-center text-xs text-muted-foreground">
              {block.alt}
            </figcaption>
          )}
        </figure>
      );

    case 'MARKDOWN':
    default:
      // Rendu texte brut avec préservation des sauts de ligne.
      // Le Markdown natif sera ajouté渐次 avec une lib légère si besoin.
      return (
        <p className="text-base leading-relaxed text-foreground whitespace-pre-wrap">
          {block.content}
        </p>
      );
  }
}
