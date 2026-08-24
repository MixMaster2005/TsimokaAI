import { useState, type FormEvent } from 'react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useCreateEspace } from '@/features/espaces/api/use-create-espace';

export const Route = createFileRoute('/onboarding/creer-espace')({
  component: CreerEspace,
});

function CreerEspace() {
  const [name, setName] = useState('');
  const [subjectTag, setSubjectTag] = useState('');
  const [description, setDescription] = useState('');
  const createEspace = useCreateEspace();
  const navigate = useNavigate();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    createEspace.mutate(
      { name, subjectTag, description },
      { onSuccess: (space) => navigate({ to: '/espaces/$spaceId', params: { spaceId: space.id } }) },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4">
      <h1 className="font-display text-xl font-semibold text-encre">Ton premier espace</h1>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">Nom</Label>
        <Input id="name" required placeholder="ex: Algorithmique S3" value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="subjectTag">Tag disciplinaire</Label>
        <Input id="subjectTag" placeholder="ex: sciences" value={subjectTag} onChange={(e) => setSubjectTag(e.target.value)} />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">Description</Label>
        <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <p className="text-xs text-encre-muted">
        Un persona pédagogique sera généré automatiquement à partir de ces informations.
      </p>
      <Button type="submit" disabled={createEspace.isPending}>
        {createEspace.isPending ? 'Création…' : 'Créer'}
      </Button>
    </form>
  );
}
