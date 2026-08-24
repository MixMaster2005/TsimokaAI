import { cn } from '@/lib/utils';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { parseFicheContent, type Fiche as FicheType } from '../types';

/**
 * CONTRAT DE COMPOSANT (design contract §9.1) : toute représentation d'une
 * fiche — dans le chat (citation), le dashboard, la gamification (badge),
 * le partage — réutilise CE composant. Aucune carte ad hoc ailleurs.
 *
 * Porté depuis le prototype HTML (tsimokaai-layout-etudiant.html) — même
 * structure, mêmes tokens, maintenant en composants React réels.
 */

interface FicheCardProps {
  fiche: FicheType;
  subjectTag?: string | null; // vient de l'espace parent (Space.subjectTag), pas de la fiche elle-même
  className?: string;
}

export function FicheCard({ fiche, subjectTag, className }: FicheCardProps) {
  const content = parseFicheContent(fiche);

  return (
    <div className={cn('flex overflow-hidden rounded-fiche border border-papier-border bg-papier-carte shadow-sm', className)}>
      <div className={cn('w-2 flex-none', getTagColorClass(subjectTag))} />
      <div className="flex-1 p-5">
        {fiche.obsolete && (
          <span className="mb-2 inline-flex items-center rounded-full bg-attention px-2.5 py-0.5 font-mono text-[0.65rem] uppercase tracking-wide text-white">
            Obsolète
          </span>
        )}
        <p className="font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">Fiche</p>
        <h3 className="mb-4 font-display text-xl font-semibold text-encre">{fiche.title}</h3>

        {content ? (
          <div className="flex flex-col gap-4">
            <Section label="Définition">
              <p className="text-sm leading-relaxed text-encre">{content.definition}</p>
            </Section>
            <Section label="Points clés">
              <ul className="list-inside list-disc text-sm leading-relaxed text-encre">
                {content.key_points.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
            </Section>
            <Section label="Exemple appliqué">
              <p className="text-sm leading-relaxed text-encre">{content.example}</p>
            </Section>
          </div>
        ) : (
          <p className="text-sm text-erreur">Contenu de la fiche illisible (JSON invalide).</p>
        )}

        <div className="mt-4 border-t border-dashed border-papier-border pt-3 font-mono text-[0.68rem] text-encre-muted">
          {fiche.sourceDocumentIds.length} document{fiche.sourceDocumentIds.length > 1 ? 's' : ''} source
          {fiche.sourceDocumentIds.length > 1 ? 's' : ''}
        </div>
      </div>
    </div>
  );
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <h4 className="mb-1 font-mono text-[0.68rem] uppercase tracking-wide text-tag-sciences">{label}</h4>
      {children}
    </div>
  );
}

/**
 * Variante "chip" — sous un message assistant, pour tracer les sources
 * citées (CDC §4.3). Ne montre volontairement pas tout le contenu.
 */
export function FicheCitationChip({ fiche, subjectTag }: { fiche: FicheType; subjectTag?: string | null }) {
  return (
    <div className="flex max-w-56 items-stretch gap-2 rounded-fiche bg-papier-carte py-1.5 pr-2.5 text-encre">
      <span className={cn('w-1 flex-none rounded-full', getTagColorClass(subjectTag))} />
      <div className="min-w-0 text-xs leading-tight">
        <p className="truncate font-mono text-[0.6rem] uppercase text-encre-muted">{fiche.title}</p>
      </div>
    </div>
  );
}

/**
 * Variante "sceau" — gamification, badges. Le contenu réel du badge (icône,
 * libellé) vient de features/gamification, ce composant ne fournit que la
 * forme circulaire cohérente avec le composant Fiche plein format.
 */
export function FicheBadgeSeal({
  colorClass,
  label,
  locked,
}: {
  colorClass: string;
  label: string;
  locked?: boolean;
}) {
  return (
    <div
      className={cn(
        'flex size-14 flex-none items-center justify-center rounded-full text-center font-mono text-[0.55rem] uppercase leading-tight text-white',
        locked ? 'border-2 border-dashed border-papier-border text-encre-muted' : colorClass,
      )}
    >
      {label}
    </div>
  );
}
