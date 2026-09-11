import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { SpineCardEnseignant } from '@/features/espaces/components/SpineCardEnseignant';

export const Route = createFileRoute('/enseignant/')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { data: espaces } = useEspacesAll();

  return (
    <div className="p-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue enseignant</p>
          <h1 className="font-display text-2xl font-semibold text-encre">Espaces de cours</h1>
        </div>
        <CreateEspaceModal trigger={<Button>Créer un espace</Button>} />
      </div>

      {espaces?.length === 0 && (
        <p className="text-sm text-encre-muted">Aucun espace créé pour l'instant.</p>
      )}

      {espaces && espaces.length > 0 && (
        <div className="flex gap-4 overflow-x-auto py-5">
          {espaces.map((space) => (
            <SpineCardEnseignant key={space.id} space={space} />
          ))}
        </div>
      )}
    </div>
  );
}
