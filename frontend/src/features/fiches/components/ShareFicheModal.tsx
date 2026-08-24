import { useState } from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { ApiError } from '@/lib/api-client';
import { cn } from '@/lib/utils';
import { useShareFiche } from '../api/use-share-fiche';

interface ShareTarget {
  id: string;
  label: string;
}

interface ShareFicheModalProps {
  ficheId: string;
  /** Groupes de l'espace + membres extérieurs : les deux cibles possibles du partage. */
  groupes: ShareTarget[];
  membres: ShareTarget[];
  trigger: React.ReactNode;
}

/**
 * Partage la fiche à un groupe de travail OU un membre de l'espace
 * (POST /api/v1/fiches/{id}/share — exactement une cible, contrôle back).
 * Pas de composant Select dans components/ui et pas d'annuaire d'utilisateurs
 * global : radios natifs sur les cibles connues localement (groupes et membres
 * de CET espace), c'est le périmètre sain du partage.
 */
export function ShareFicheModal({ ficheId, groupes, membres, trigger }: ShareFicheModalProps) {
  const [open, setOpen] = useState(false);
  const [cible, setCible] = useState('');
  const shareFiche = useShareFiche(ficheId);

  const options = [
    ...groupes.map((g) => ({ value: `groupe:${g.id}`, label: `Groupe · ${g.label}` })),
    ...membres.map((m) => ({ value: `membre:${m.id}`, label: m.label })),
  ];

  function handleShare() {
    if (!cible) return;
    const [type, id] = cible.split(':');
    const payload = type === 'groupe' ? { groupeId: id } : { destinataireId: id };
    shareFiche.mutate(payload, { onSuccess: () => setOpen(false) });
  }

  const messageErreur = shareFiche.error instanceof ApiError ? shareFiche.error.message : null;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <div className="flex flex-col gap-4">
          <DialogHeader>
            <DialogTitle>Partager cette fiche</DialogTitle>
            <DialogDescription>
              Choisis un groupe de travail ou un membre de l'espace.
            </DialogDescription>
          </DialogHeader>

          <div className="flex max-h-64 flex-col gap-1 overflow-y-auto" role="radiogroup">
            {options.map((o) => (
              <label
                key={o.value}
                className="flex cursor-pointer items-center gap-2 rounded-fiche border border-transparent px-2 py-1.5 text-sm hover:bg-secondary has-checked:border-border"
              >
                <input
                  type="radio"
                  name="share-target"
                  className="accent-primary"
                  checked={cible === o.value}
                  onChange={() => setCible(o.value)}
                />
                <span className="truncate">{o.label}</span>
              </label>
            ))}
            {options.length === 0 && (
              <p className="text-xs text-encre-muted">
                Aucun groupe ni membre dans cet espace — crée-en un d'abord (onglet Membres).
              </p>
            )}
          </div>
          {messageErreur && (
            <p className={cn('text-xs text-erreur')}>{messageErreur}</p>
          )}

          <DialogFooter>
            <Button disabled={!cible || shareFiche.isPending} onClick={handleShare}>
              {shareFiche.isPending ? 'Partage…' : 'Partager'}
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  );
}
