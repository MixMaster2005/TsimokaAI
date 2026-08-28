import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useSession } from '@/features/auth/api/use-session';
import { useUpdateProfile } from '@/features/auth/api/use-update-profile';

export const Route = createFileRoute('/enseignant/parametres')({
  component: ParametresEnseignant,
});

function ParametresEnseignant() {
  const { data: user } = useSession();
  const updateProfile = useUpdateProfile();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!displayName.trim()) return;
    updateProfile.mutate({ displayName: displayName.trim() });
  }

  return (
    <div className="max-w-lg p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Compte Enseignant</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Paramètres</h1>

      <Card>
        <CardHeader>
          <CardTitle>Profil</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="displayName">Nom affiché</Label>
              <Input
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Email</Label>
              <Input value={user?.email ?? ''} disabled />
            </div>
            <Button type="submit" disabled={updateProfile.isPending} className="self-start">
              {updateProfile.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
