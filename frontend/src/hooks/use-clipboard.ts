import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Copie presse-papiers avec filet (Lot 5) : try/catch + message live + repli.
 * En cas d'échec, `echec` passe à true avec un message invitant à la copie
 * manuelle — l'appelant l'affiche via `role="status"`.
 */
export function useClipboard(delaiReset = 2000) {
  const [copie, setCopie] = useState(false);
  const [echec, setEchec] = useState<string | null>(null);
  const minuteur = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (minuteur.current) clearTimeout(minuteur.current);
    };
  }, []);

  const copier = useCallback(
    async (texte: string) => {
      setEchec(null);
      try {
        await navigator.clipboard.writeText(texte);
        setCopie(true);
        if (minuteur.current) clearTimeout(minuteur.current);
        minuteur.current = setTimeout(() => setCopie(false), delaiReset);
        return true;
      } catch {
        setEchec('Copie impossible — sélectionne le texte manuellement puis réessaie.');
        return false;
      }
    },
    [delaiReset],
  );

  return { copie, echec, copier };
}
