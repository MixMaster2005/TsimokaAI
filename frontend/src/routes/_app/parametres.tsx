import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { useSession } from '@/features/auth/api/use-session';
import { useDeleteAccount, useUpdateProfile } from '@/features/auth/api/use-update-profile';

export const Route = createFileRoute('/_app/parametres')({
  component: Parametres,
});

function Parametres() {
  const { data: user } = useSession();
  const updateProfile = useUpdateProfile();
  const deleteAccount = useDeleteAccount();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [confirmDelete, setConfirmDelete] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    updateProfile.mutate({ displayName });
  }

  return (
    <div className="max-w-lg p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Compte</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Paramètres</h1>

      <Card>
        <CardHeader>
          <CardTitle>Profil</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="displayName">Nom affiché</Label>
              <Input id="displayName" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Email</Label>
              {/* Pas d'endpoint de changement d'email identifié côté back — lecture seule pour l'instant */}
              <Input value={user?.email ?? ''} disabled />
            </div>
            <Button type="submit" disabled={updateProfile.isPending} className="self-start">
              {updateProfile.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </form>

          <Separator className="my-6" />

          <div>
            <p className="mb-2 text-xs text-encre-muted">
              Supprimer ton compte efface aussi tes espaces, fiches et conversations. Irréversible.
            </p>
            {confirmDelete ? (
              <div className="flex gap-2">
                <Button variant="destructive" onClick={() => deleteAccount.mutate()}>
                  Confirmer la suppression
                </Button>
                <Button variant="outline" onClick={() => setConfirmDelete(false)}>
                  Annuler
                </Button>
              </div>
            ) : (
              <Button variant="destructive" onClick={() => setConfirmDelete(true)}>
                Supprimer mon compte
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
