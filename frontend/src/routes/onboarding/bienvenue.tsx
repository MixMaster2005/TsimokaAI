import { useState } from 'react';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { JoinEspaceModal } from '@/features/espaces/components/JoinEspaceModal';
import { useUpdateProfile } from '@/features/auth/api/use-update-profile';
import type { UserRole } from '@/features/auth/types';

export const Route = createFileRoute('/onboarding/bienvenue')({
  component: Bienvenue,
});

function Bienvenue() {
  const navigate = useNavigate();
  const updateProfile = useUpdateProfile();
  const [roleChosen, setRoleChosen] = useState(false);

  function chooseRole(role: UserRole) {
    updateProfile.mutate(
      { role },
      {
        onSuccess: () => setRoleChosen(true),
      },
    );
  }

  // Étape 1 : choix du rôle
  if (!roleChosen) {
    return (
      <div className="flex max-w-md flex-col items-center gap-6 text-center">
        <h1 className="font-display text-2xl font-semibold text-encre">
          Bienvenue sur TsimokaAI
        </h1>
        <p className="text-sm text-encre-muted">
          Tu es étudiant ou enseignant ?
        </p>
        <div className="flex w-full flex-col gap-2">
          <Button onClick={() => chooseRole('STUDENT')} disabled={updateProfile.isPending}>
            {updateProfile.isPending && updateProfile.variables?.role === 'STUDENT'
              ? 'Chargement…'
              : 'Étudiant'}
          </Button>
          <Button
            variant="outline"
            onClick={() => chooseRole('ENSEIGNANT')}
            disabled={updateProfile.isPending}
          >
            {updateProfile.isPending && updateProfile.variables?.role === 'ENSEIGNANT'
              ? 'Chargement…'
              : 'Enseignant'}
          </Button>
        </div>
      </div>
    );
  }

  // Étape 2 : création / join espace
  return (
    <div className="flex max-w-md flex-col items-center gap-6 text-center">
      <h1 className="font-display text-2xl font-semibold text-encre">Presque fini !</h1>
      <p className="text-sm text-encre-muted">
        Crée ton premier espace de cours, ou rejoins-en un si ton enseignant t'a déjà donné un code.
      </p>
      <div className="flex w-full flex-col gap-2">
        <Button asChild>
          <Link to="/onboarding/creer-espace">Créer mon premier espace</Link>
        </Button>
        <JoinEspaceModal
          trigger={<Button variant="outline">Rejoindre un espace via un code</Button>}
          onJoined={(space) =>
            navigate({ to: '/espaces/$spaceId', params: { spaceId: space.id } })
          }
        />
      </div>
      <Link to="/" className="text-xs text-encre-muted hover:text-encre">
        Passer cette étape
      </Link>
    </div>
  );
}
