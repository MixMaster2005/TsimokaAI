import { useState, type FormEvent } from 'react';

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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api-client';
import { useJoinEspace } from '../api/use-join-espace';
import type { Space } from '../types';

interface JoinEspaceModalProps {
  trigger: React.ReactNode;
  onJoined?: (space: Space) => void;
}

const CODE_MESSAGE_ERREUR: Record<number, string> = {
  404: "Aucun espace ne correspond à ce code — vérifie-le auprès de ton enseignant.",
  409: 'Tu fais déjà partie de cet espace.',
};

export function JoinEspaceModal({ trigger, onJoined }: JoinEspaceModalProps) {
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState('');
  const joinEspace = useJoinEspace();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    joinEspace.mutate(
      code,
      {
        onSuccess: (space) => {
          setOpen(false);
          setCode('');
          onJoined?.(space);
        },
      },
    );
  }

  const messageErreur =
    joinEspace.error instanceof ApiError
      ? (CODE_MESSAGE_ERREUR[joinEspace.error.status] ?? joinEspace.error.message)
      : null;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <DialogHeader>
            <DialogTitle>Rejoindre un espace</DialogTitle>
            <DialogDescription>
              Entre le code d'invitation donné par le propriétaire de l'espace (8 caractères).
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="code">Code d'invitation</Label>
            {/* autoCapitalize + style mono : le code se recopie à la main, on aide au maximum */}
            <Input
              id="code"
              required
              autoFocus
              maxLength={8}
              placeholder="ex : A7K2M9XQ"
              className="font-mono uppercase tracking-widest"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
            />
            {messageErreur && <p className="text-xs text-erreur">{messageErreur}</p>}
          </div>

          <DialogFooter>
            <Button type="submit" disabled={joinEspace.isPending}>
              {joinEspace.isPending ? 'Adhésion…' : 'Rejoindre'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
