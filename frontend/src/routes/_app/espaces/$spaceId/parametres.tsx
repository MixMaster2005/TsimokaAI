import { useState, type FormEvent } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { useEspace } from '@/features/espaces/api/use-espace';
import { useDeleteEspace, useUpdateEspace } from '@/features/espaces/api/use-update-espace';

/**
 * ⚠️ Ce fichier ne fait PAS le guard "créateur uniquement" pour l'instant —
 * l'onglet Paramètres est listé dans routes/_app/espaces/$spaceId/route.tsx
 * sans condition d'affichage. TODO : dans EspaceLayout, comparer
 * space.userId à session.user.id (useSession) et masquer l'onglet sinon —
 * exactement le point ouvert noté dans route.tsx du layout Espace.
 */
export const Route = createFileRoute('/_app/espaces/$spaceId/parametres')({
  component: ParametresEspace,
});

function ParametresEspace() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/parametres' });
  const { data: space } = useEspace(spaceId);
  const updateEspace = useUpdateEspace(spaceId);
  const deleteEspace = useDeleteEspace(spaceId);
  const [name, setName] = useState(space?.name ?? '');
  const [description, setDescription] = useState(space?.description ?? '');
  const [subjectTag, setSubjectTag] = useState(space?.subjectTag ?? '');
  const [confirmDelete, setConfirmDelete] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    updateEspace.mutate({ name, description, subjectTag });
  }

  if (!space) return null;

  return (
    <div className="max-w-lg p-6">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="name">Nom</Label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="subjectTag">Tag disciplinaire</Label>
          <Input id="subjectTag" value={subjectTag} onChange={(e) => setSubjectTag(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="description">Description</Label>
          <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label>Persona pédagogique généré</Label>
          {/* Lecture seule pour l'instant — pas de endpoint de régénération identifié côté back */}
          <p className="rounded-fiche border border-border bg-secondary p-3 text-xs text-muted-foreground">
            {space.assistantPersona ?? 'Non généré pour l\u2019instant.'}
          </p>
        </div>
        <Button type="submit" disabled={updateEspace.isPending} className="self-start">
          Enregistrer
        </Button>
      </form>

      <Separator className="my-6" />

      <div>
        <p className="mb-2 text-xs text-muted-foreground">
          Supprimer cet espace efface aussi ses documents, fiches et conversations. Irréversible.
        </p>
        {confirmDelete ? (
          <div className="flex gap-2">
            <Button variant="destructive" onClick={() => deleteEspace.mutate()}>
              Confirmer la suppression
            </Button>
            <Button variant="outline" onClick={() => setConfirmDelete(false)}>
              Annuler
            </Button>
          </div>
        ) : (
          <Button variant="destructive" onClick={() => setConfirmDelete(true)}>
            Supprimer l'espace
          </Button>
        )}
      </div>
    </div>
  );
}
