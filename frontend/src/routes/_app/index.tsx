import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { espacesQueryOptions } from '@/features/espaces/api/use-espaces';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { EtagereGrid } from '@/features/espaces/components/EtagereGrid';

export const Route = createFileRoute('/_app/')({
  // Précharge la liste des espaces AVANT que la page s'affiche — quand le
  // composant monte, useEspaces() dans EtagereGrid retombe direct sur du
  // cache déjà chaud, pas de spinner si la navigation s'est faite via <Link>.
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesQueryOptions),
  component: Etagere,
});

function Etagere() {
  return (
    <div>
      <div className="flex items-start justify-between px-8 pt-7">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Mes espaces</p>
          <h1 className="font-display text-2xl font-semibold text-encre">L'étagère</h1>
        </div>
        <CreateEspaceModal trigger={<Button>Créer un espace</Button>} />
      </div>

      {/* TODO : bloc "Reprendre où j'en étais" (cf. cartographie UI) —
          nécessite un endpoint de dernière activité côté chat-service,
          pas encore identifié au moment du scaffolding. */}

      <EtagereGrid />
    </div>
  );
}
