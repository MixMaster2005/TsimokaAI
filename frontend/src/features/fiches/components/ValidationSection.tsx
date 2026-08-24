import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { useSession } from '@/features/auth/api/use-session';
import { validationQueryOptions, useValidateFiche } from '../api/use-validation';
import type { ValidationStatut } from '../types';

/**
 * État de validation enseignant de la fiche + actions de verdict.
 *
 * Le tampon suit le contrat de design : VALIDÉE / À REVOIR en Plex Mono
 * majuscules, légèrement pivoté, en --succes / --attention (jamais rouge/vert
 * alarmiste). Les boutons de verdict ne s'affichent que pour un ADMIN — le
 * back refuse de toute façon (403), on évite juste l'appel voué à échouer.
 */
export function ValidationSection({ ficheId }: { ficheId: string }) {
  const { data: session } = useSession();
  const isTeacher = session?.role === 'ADMIN';
  const { data: validation } = useQuery(validationQueryOptions(ficheId));
  const validateFiche = useValidateFiche(ficheId);
  const [commentaire, setCommentaire] = useState('');
  const [formulaireOuvert, setFormulaireOuvert] = useState(false);

  function statutLabel(statut: ValidationStatut) {
    switch (statut) {
      case 'VALIDEE':
        return 'Validée';
      case 'REJETEE':
        return 'À revoir';
      default:
        return 'En attente';
    }
  }

  function soumettre(statut: ValidationStatut) {
    validateFiche.mutate(
      // commentaire vide = absent
      { statut, ...(commentaire.trim() ? { commentaire: commentaire.trim() } : {}) },
      {
        onSuccess: () => {
          setCommentaire('');
          setFormulaireOuvert(false);
        },
      },
    );
  }

  return (
    <section className="flex flex-col gap-3">
      <h2 className="font-display text-sm font-semibold text-encre">Validation</h2>

      {!validation && (
        <p className="text-xs text-encre-muted">
          {isTeacher ? 'Aucun verdict pour le moment.' : 'Fiche pas encore validée par un enseignant.'}
        </p>
      )}

      {validation && (
        <div className="flex items-center gap-3">
          {/* Tampon : Plex Mono majuscules, légère rotation, cf. contrat de design §validation */}
          <span
            className={`inline-block -rotate-3 rounded-fiche border px-2.5 py-1 font-mono text-xs font-semibold uppercase tracking-widest ${
              validation.statut === 'VALIDEE'
                ? 'border-succes/40 text-succes'
                : validation.statut === 'REJETEE'
                  ? 'border-attention/50 text-attention'
                  : 'border-border text-encre-muted'
            }`}
          >
            {statutLabel(validation.statut)}
          </span>
          {validation.commentaire && (
            <p className="min-w-0 flex-1 truncate text-xs text-encre-muted" title={validation.commentaire}>
              {validation.commentaire}
            </p>
          )}
        </div>
      )}

      {isTeacher && (
        <>
          {!formulaireOuvert ? (
            <Button variant="outline" size="sm" onClick={() => setFormulaireOuvert(true)}>
              Donner un verdict
            </Button>
          ) : (
            <div className="flex flex-col gap-2 rounded-fiche border border-papier-border bg-papier-carte p-3">
              <textarea
                placeholder="Commentaire (optionnel)"
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
                rows={2}
                className="w-full resize-none rounded-md border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none"
              />
              <div className="flex gap-2 self-end">
                <Button variant="ghost" size="sm" disabled={validateFiche.isPending} onClick={() => setFormulaireOuvert(false)}>
                  Annuler
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={validateFiche.isPending}
                  onClick={() => soumettre('REJETEE')}
                >
                  À revoir
                </Button>
                <Button size="sm" disabled={validateFiche.isPending} onClick={() => soumettre('VALIDEE')}>
                  Valider
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  );
}
