import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { useSession } from '@/features/auth/api/use-session';
import { useUpdateProfile } from '@/features/auth/api/use-update-profile';
import { useDeleteAccount } from '@/features/auth/api/use-delete-account';
import { useChangePassword } from '@/features/auth/api/use-change-password';

export const Route = createFileRoute('/_app/parametres')({
  component: Parametres,
});

function Parametres() {
  const { data: user } = useSession();
  const updateProfile = useUpdateProfile();
  const deleteAccount = useDeleteAccount();
  const changePassword = useChangePassword();

  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [confirmDelete, setConfirmDelete] = useState(false);

  const [ancienMotDePasse, setAncienMotDePasse] = useState('');
  const [nouveauMotDePasse, setNouveauMotDePasse] = useState('');
  const [confirmMotDePasse, setConfirmMotDePasse] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  function handleProfileSubmit(e: FormEvent) {
    e.preventDefault();
    updateProfile.mutate({ displayName });
  }

  function handlePasswordSubmit(e: FormEvent) {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess(false);

    if (nouveauMotDePasse !== confirmMotDePasse) {
      setPasswordError('Les mots de passe ne correspondent pas');
      return;
    }
    if (nouveauMotDePasse.length < 8) {
      setPasswordError('Le mot de passe doit faire au moins 8 caractères');
      return;
    }

    changePassword.mutate(
      { ancienMotDePasse, nouveauMotDePasse },
      {
        onSuccess: () => {
          setAncienMotDePasse('');
          setNouveauMotDePasse('');
          setConfirmMotDePasse('');
          setPasswordSuccess(true);
        },
        onError: (error) => {
          setPasswordError(error.message || 'Erreur lors du changement de mot de passe');
        },
      },
    );
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
          <form onSubmit={handleProfileSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="displayName">Nom affiché</Label>
              <Input id="displayName" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Email</Label>
              <Input value={user?.email ?? ''} disabled />
            </div>
            <Button type="submit" disabled={updateProfile.isPending} className="self-start">
              {updateProfile.isPending ? 'Enregistrement…' : 'Enregistrer'}
            </Button>
          </form>

          <Separator className="my-6" />

          <form onSubmit={handlePasswordSubmit} className="flex flex-col gap-3">
            <h3 className="font-display text-sm font-semibold text-encre">Mot de passe</h3>
            <div className="flex flex-col gap-2">
              <Input
                type="password"
                placeholder="Mot de passe actuel"
                value={ancienMotDePasse}
                onChange={(e) => setAncienMotDePasse(e.target.value)}
                required
              />
              <Input
                type="password"
                placeholder="Nouveau mot de passe (min. 8 caractères)"
                value={nouveauMotDePasse}
                onChange={(e) => setNouveauMotDePasse(e.target.value)}
                required
              />
              <Input
                type="password"
                placeholder="Confirmer le nouveau mot de passe"
                value={confirmMotDePasse}
                onChange={(e) => setConfirmMotDePasse(e.target.value)}
                required
              />
            </div>
            {passwordError && <p className="text-xs text-destructive">{passwordError}</p>}
            {passwordSuccess && <p className="text-xs text-succes">Mot de passe modifié avec succès.</p>}
            <Button
              type="submit"
              disabled={changePassword.isPending || !ancienMotDePasse || !nouveauMotDePasse}
              className="self-start"
            >
              {changePassword.isPending ? 'Modification…' : 'Changer le mot de passe'}
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
