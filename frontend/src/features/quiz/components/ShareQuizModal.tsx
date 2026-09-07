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
import { useShareQuiz } from '../api/use-share-quiz';

interface ShareTarget {
  id: string;
  label: string;
}

interface ShareQuizModalProps {
  quizId: string;
  membres: ShareTarget[];
  trigger: React.ReactNode;
}

export function ShareQuizModal({ quizId, membres, trigger }: ShareQuizModalProps) {
  const [open, setOpen] = useState(false);
  const [cible, setCible] = useState('');
  const shareQuiz = useShareQuiz(quizId);

  function handleShare() {
    if (!cible) return;
    shareQuiz.mutate({ destinataireId: cible }, { onSuccess: () => setOpen(false) });
  }

  const messageErreur = shareQuiz.error instanceof ApiError ? shareQuiz.error.message : null;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <div className="flex flex-col gap-4">
          <DialogHeader>
            <DialogTitle>Partager ce quiz</DialogTitle>
            <DialogDescription>
              Choisissez un membre de l'espace à qui partager ce quiz.
            </DialogDescription>
          </DialogHeader>

          <div className="flex max-h-64 flex-col gap-1 overflow-y-auto" role="radiogroup">
            {membres.map((m) => (
              <label
                key={m.id}
                className="flex cursor-pointer items-center gap-2 rounded-fiche border border-transparent px-2 py-1.5 text-sm hover:bg-secondary has-checked:border-border"
              >
                <input
                  type="radio"
                  name="share-target"
                  className="accent-primary"
                  checked={cible === m.id}
                  onChange={() => setCible(m.id)}
                />
                <span className="truncate">{m.label}</span>
              </label>
            ))}
            {membres.length === 0 && (
              <p className="text-xs text-muted-foreground">
                Aucun membre dans cet espace — créez-en un d'abord (onglet Membres).
              </p>
            )}
          </div>
          {messageErreur && (
            <p className={cn('text-xs text-erreur')}>{messageErreur}</p>
          )}

          <DialogFooter>
            <Button disabled={!cible || shareQuiz.isPending} onClick={handleShare}>
              {shareQuiz.isPending ? 'Partage…' : 'Partager'}
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  );
}
