import { createFileRoute, Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { useMembres } from '@/features/espaces/api/use-membres';
import { useDocuments } from '@/features/documents/api/use-documents';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { getTagColorClass } from '@/features/espaces/lib/get-tag-color';
import { cn } from '@/lib/utils';
import type { Space } from '@/features/espaces/types';

/**
 * Tableau de bord ENSEIGNANT — vue de supervision : tous les espaces de la
 * plateforme (GET /api/v1/spaces/all), point d'entrée vers les fiches à valider.
 *
 * Limite : les agrégats "chapitres difficiles avec densité d'encre" du contrat
 * de design nécessitent des endpoints analytiques non implémentés côté
 * analytics-service — voir README frontend, section Limitations connues.
 */
export const Route = createFileRoute('/enseignant/')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: TableauDeBordEnseignant,
});

function EspaceEnseignantCard({ espace }: { espace: Space }) {
  const { data: membres } = useMembres(espace.id);
  const { data: documents } = useDocuments(espace.id);

  const nbEtudiants = membres?.length ?? 0;
  const nbDocs = documents?.length ?? espace.documentCount ?? 0;

  return (
    <Link
      to="/enseignant/espaces/$spaceId"
      params={{ spaceId: espace.id }}
      className="flex items-center gap-3 rounded-fiche border border-papier-border bg-papier-carte p-4 hover:bg-secondary"
    >
      <span className={cn('w-1.5 self-stretch rounded-full', getTagColorClass(espace.subjectTag))} />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-encre">{espace.name}</p>
        <p className="font-mono text-[0.68rem] text-encre-muted">
          {espace.subjectTag ?? 'sans tag'}
        </p>
        <div className="mt-2 flex items-center gap-3 font-mono text-[0.65rem] text-encre-muted">
          <span>👥 {nbEtudiants} étudiant{nbEtudiants > 1 ? 's' : ''}</span>
          <span>📄 {nbDocs} doc{nbDocs > 1 ? 's' : ''}</span>
        </div>
      </div>
    </Link>
  );
}

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

      <div className="grid grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-3">
        {espaces?.map((espace) => (
          <EspaceEnseignantCard key={espace.id} espace={espace} />
        ))}
      </div>
    </div>
  );
}
