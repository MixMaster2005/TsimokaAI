import { useEffect, useRef, useState } from 'react';

interface MermaidBlockProps {
  chart: string;
}

export function MermaidBlock({ chart }: MermaidBlockProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function render() {
      try {
        const mermaid = (await import('mermaid')).default;
        mermaid.initialize({
          startOnLoad: false,
          theme: 'default',
          securityLevel: 'loose',
        });
        if (cancelled || !containerRef.current) return;

        const id = `mermaid-${Math.random().toString(36).slice(2, 9)}`;
        const { svg } = await mermaid.render(id, chart);
        if (!cancelled && containerRef.current) {
          containerRef.current.innerHTML = svg;
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Erreur de rendu Mermaid');
        }
      }
    }

    render();
    return () => { cancelled = true; };
  }, [chart]);

  if (error) {
    return (
      <div className="my-3 rounded-md border border-border bg-muted p-3">
        <p className="mb-1 text-xs text-muted-foreground">Erreur Mermaid</p>
        <pre className="overflow-x-auto text-sm text-foreground">{chart}</pre>
      </div>
    );
  }

  return (
    <div className="my-3 flex justify-center overflow-x-auto rounded-md border border-border bg-background p-3">
      <div ref={containerRef} />
    </div>
  );
}
