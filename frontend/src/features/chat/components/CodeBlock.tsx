import { useState } from 'react';

interface CodeBlockProps {
  language?: string | null;
  children: string;
}

export function CodeBlock({ language, children }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(children);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="my-3 rounded-md border border-border bg-muted">
      <div className="flex items-center justify-between border-b border-border px-3 py-1.5">
        <span className="font-mono text-xs text-muted-foreground">{language ?? 'code'}</span>
        <button
          type="button"
          onClick={handleCopy}
          className="font-mono text-xs text-muted-foreground hover:text-foreground"
        >
          {copied ? 'Copié !' : 'Copier'}
        </button>
      </div>
      <pre className="overflow-x-auto p-3 text-sm">
        <code className="font-mono text-foreground">{children}</code>
      </pre>
    </div>
  );
}
