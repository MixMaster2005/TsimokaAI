import { useEffect, useRef, useState } from 'react';

interface MathBlockProps {
  math: string;
  display?: boolean;
}

export function MathBlock({ math, display = false }: MathBlockProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function render() {
      try {
        const katex = (await import('katex')).default;
        if (cancelled || !containerRef.current) return;
        containerRef.current.innerHTML = katex.renderToString(math, {
          displayMode: display,
          throwOnError: false,
        });
      } catch {
        if (!cancelled) setError(true);
      }
    }

    render();
    return () => { cancelled = true; };
  }, [math, display]);

  if (error) {
    return (
      <code className="my-1 rounded bg-muted px-1 py-0.5 font-mono text-sm text-foreground">
        {display ? `$$${math}$$` : `$${math}$`}
      </code>
    );
  }

  return display ? (
    <div ref={containerRef} className="my-3 overflow-x-auto text-center" />
  ) : (
    <span ref={containerRef} className="inline-block" />
  );
}
