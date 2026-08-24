import { useEffect, useState } from 'react';

/**
 * Contrat de design §7 (Mouvement) : streaming façon craie pour les
 * réponses assistant, avec repli instantané si prefers-reduced-motion.
 * Comme la réponse arrive d'un bloc (pas de vrai streaming réseau, voir
 * use-send-message.ts), cet effet est purement une révélation progressive
 * côté client d'un texte déjà complet.
 */
export function useChalkReveal(text: string, active: boolean) {
  const [visibleWords, setVisibleWords] = useState(active ? 0 : text.split(' ').length);

  useEffect(() => {
    if (!active) {
      setVisibleWords(text.split(' ').length);
      return;
    }
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) {
      setVisibleWords(text.split(' ').length);
      return;
    }

    const words = text.split(' ');
    setVisibleWords(0);
    let i = 0;
    const interval = setInterval(() => {
      i += 1;
      setVisibleWords(i);
      if (i >= words.length) clearInterval(interval);
    }, 45);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text, active]);

  return text.split(' ').slice(0, visibleWords).join(' ');
}
