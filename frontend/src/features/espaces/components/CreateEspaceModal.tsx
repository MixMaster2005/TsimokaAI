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
import { useCreateEspace } from '../api/use-create-espace';

interface CreateEspaceModalProps {
  trigger: React.ReactNode;
  onCreated?: (spaceId: string) => void;
}

export function CreateEspaceModal({ trigger, onCreated }: CreateEspaceModalProps) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [subjectTag, setSubjectTag] = useState('');
  const createEspace = useCreateEspace();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    createEspace.mutate(
      { name, description, subjectTag },
      {
        onSuccess: (space) => {
          setOpen(false);
          setName('');
          setDescription('');
          setSubjectTag('');
          onCreated?.(space.id);
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <DialogHeader>
            <DialogTitle>Créer un espace</DialogTitle>
            <DialogDescription>
              Un persona pédagogique sera généré automatiquement à partir de ces informations.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="name">Nom</Label>
            <Input id="name" required value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="subjectTag">Tag disciplinaire</Label>
            <Input
              id="subjectTag"
              placeholder="ex: sciences, lettres, eco…"
              value={subjectTag}
              onChange={(e) => setSubjectTag(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="description">Description</Label>
            <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="submit" disabled={createEspace.isPending}>
              {createEspace.isPending ? 'Création…' : 'Créer'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
