import type { ReactNode } from 'react';

import type { ContentBlock } from '../types';
import { CodeBlock } from './CodeBlock';
import { MermaidBlock } from './MermaidBlock';
import { MathBlock } from './MathBlock';

interface MessageRendererProps {
  blocks: ContentBlock[];
}

/**
 * Rend une liste de blocs structurés parsés côté backend.
 * Les blocs MARKDOWN passent par un renderer local volontairement strict :
 * aucun HTML brut, seulement la syntaxe courante utile aux réponses pédagogiques.
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
      return <MarkdownBlock content={block.content ?? ''} />;
  }
}

function MarkdownBlock({ content }: { content: string }) {
  const lines = content.split(/\r?\n/);
  const nodes: ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (!line.trim()) {
      i += 1;
      continue;
    }

    if (isTableStart(lines, i)) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].includes('|') && lines[i].trim()) {
        tableLines.push(lines[i]);
        i += 1;
      }
      nodes.push(<MarkdownTable key={nodes.length} lines={tableLines} />);
      continue;
    }

    const heading = /^(#{1,4})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const text = renderInline(heading[2]);
      const className = 'mt-4 first:mt-0 font-semibold leading-tight text-foreground';
      if (level === 1) nodes.push(<h2 key={nodes.length} className={`${className} text-xl`}>{text}</h2>);
      else if (level === 2) nodes.push(<h3 key={nodes.length} className={`${className} text-lg`}>{text}</h3>);
      else nodes.push(<h4 key={nodes.length} className={`${className} text-base`}>{text}</h4>);
      i += 1;
      continue;
    }

    if (/^\s*[-*]\s+/.test(line)) {
      const items: ReactNode[] = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
        items.push(<li key={items.length}>{renderInline(lines[i].replace(/^\s*[-*]\s+/, ''))}</li>);
        i += 1;
      }
      nodes.push(<ul key={nodes.length} className="my-2 ml-5 list-disc space-y-1">{items}</ul>);
      continue;
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items: ReactNode[] = [];
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(<li key={items.length}>{renderInline(lines[i].replace(/^\s*\d+\.\s+/, ''))}</li>);
        i += 1;
      }
      nodes.push(<ol key={nodes.length} className="my-2 ml-5 list-decimal space-y-1">{items}</ol>);
      continue;
    }

    if (/^>\s?/.test(line)) {
      const quoted: string[] = [];
      while (i < lines.length && /^>\s?/.test(lines[i])) {
        quoted.push(lines[i].replace(/^>\s?/, ''));
        i += 1;
      }
      nodes.push(
        <blockquote key={nodes.length} className="my-3 border-l-2 border-border pl-3 text-muted-foreground">
          {quoted.map((text, index) => (
            <p key={index} className="leading-relaxed">{renderInline(text)}</p>
          ))}
        </blockquote>,
      );
      continue;
    }

    const paragraph: string[] = [];
    while (i < lines.length && lines[i].trim() && !isBlockStart(lines, i)) {
      paragraph.push(lines[i]);
      i += 1;
    }
    nodes.push(
      <p key={nodes.length} className="my-2 leading-relaxed text-foreground">
        {renderInline(paragraph.join(' '))}
      </p>,
    );
  }

  return <div className="text-base leading-relaxed text-foreground">{nodes}</div>;
}

function isBlockStart(lines: string[], index: number) {
  const line = lines[index];
  return /^(#{1,4})\s+/.test(line)
    || /^\s*[-*]\s+/.test(line)
    || /^\s*\d+\.\s+/.test(line)
    || /^>\s?/.test(line)
    || isTableStart(lines, index);
}

function isTableStart(lines: string[], index: number) {
  return index + 1 < lines.length
    && lines[index].includes('|')
    && /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(lines[index + 1]);
}

function MarkdownTable({ lines }: { lines: string[] }) {
  const [headerLine, , ...bodyLines] = lines;
  const headers = splitTableRow(headerLine);
  const rows = bodyLines.map(splitTableRow);

  return (
    <div className="my-3 overflow-x-auto">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr>
            {headers.map((cell, index) => (
              <th key={index} className="border border-border bg-muted px-3 py-2 font-semibold">
                {renderInline(cell)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td key={cellIndex} className="border border-border px-3 py-2 align-top">
                  {renderInline(cell)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function splitTableRow(line: string) {
  return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((cell) => cell.trim());
}

function renderInline(text: string): ReactNode[] {
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|\[[^\]]+\]\([^)]+\))/g);

  return parts.filter(Boolean).map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code key={index} className="rounded bg-muted px-1 py-0.5 font-mono text-sm">{part.slice(1, -1)}</code>;
    }
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index} className="font-semibold">{renderInline(part.slice(2, -2))}</strong>;
    }
    if (part.startsWith('*') && part.endsWith('*')) {
      return <em key={index}>{renderInline(part.slice(1, -1))}</em>;
    }
    const link = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(part);
    if (link) {
      return (
        <a key={index} href={link[2]} target="_blank" rel="noreferrer" className="underline underline-offset-2">
          {link[1]}
        </a>
      );
    }
    return part;
  });
}
