import { useRef, useState } from 'react';

interface CodeBlockProps {
  language?: string | null;
  children: string;
}

export function CodeBlock({ language, children }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);
  const [echec, setEchec] = useState(false);
  const codeRef = useRef<HTMLElement>(null);

  const handleCopy = async () => {
    setEchec(false);
    try {
      await navigator.clipboard.writeText(children);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Presse-papiers indisponible : sélection manuelle du code comme repli.
      setEchec(true);
      const range = document.createRange();
      if (codeRef.current) {
        range.selectNodeContents(codeRef.current);
        const selection = window.getSelection();
        selection?.removeAllRanges();
        selection?.addRange(range);
      }
    }
  };

  return (
    <div className="my-3 rounded-md border border-border bg-muted">
      <div className="flex items-center justify-between border-b border-border px-3 py-1.5">
        <span className="font-mono text-xs text-muted-foreground">{language ?? 'code'}</span>
        <button
          type="button"
          onClick={handleCopy}
          className="rounded-sm font-mono text-xs text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          {copied ? 'Copié !' : 'Copier'}
        </button>
      </div>
      <p role="status" aria-live="polite" className="sr-only">
        {copied ? 'Code copié.' : echec ? 'Copie impossible — sélectionne le code manuellement.' : ''}
      </p>
      <pre className="overflow-x-auto p-3 text-sm" tabIndex={0} role="region" aria-label={`Code ${language ?? ''}`.trim()}>
        <code ref={codeRef} className="font-mono text-foreground">{children}</code>
      </pre>
    </div>
  );
}
