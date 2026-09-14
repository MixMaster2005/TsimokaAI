import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { useSession } from '@/features/auth/api/use-session';
import { espacesAllQueryOptions, useEspacesAll } from '@/features/espaces/api/use-espaces-all';
import { CreateEspaceModal } from '@/features/espaces/components/CreateEspaceModal';
import { EtagereFiltres } from '@/features/espaces/components/EtagereFiltres';
import { EtagereSection } from '@/features/espaces/components/EtagereSection';
import {
  FILTRES_ETAGERE_DEFAUT,
  extraireTags,
  filtrerEspaces,
  type FiltresEtagere,
} from '@/features/espaces/lib/filtrer-espaces';

export const Route = createFileRoute('/enseignant/')({
  loader: ({ context: { queryClient } }) => queryClient.ensureQueryData(espacesAllQueryOptions),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { data: all, isLoading, isError } = useEspacesAll();
  // Session déjà en cache via le guard enseignant : 0 requête en plus.
  const { data: session } = useSession();
  const currentUserId = session?.id;

  const [value, setValue] = useState<FiltresEtagere>(FILTRES_ETAGERE_DEFAUT);

  const filtres = filtrerEspaces(all ?? [], value, currentUserId);
  // Double garde : owner fiable depuis le fix back + comparaison userId par sécurité.
  const miens = filtres.filter(
    (s) => s.owner === true || (currentUserId !== undefined && s.userId === currentUserId),
  );
  const autres = filtres.filter(
    (s) => !(s.owner === true || (currentUserId !== undefined && s.userId === currentUserId)),
  );

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4 p-8 pb-0">
        <div>
          <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue enseignant</p>
          <h1 className="font-display text-2xl font-semibold text-encre">Espaces de cours</h1>
        </div>
        <CreateEspaceModal trigger={<Button>Créer un espace</Button>} />
      </div>

      {isLoading && (
        <p className="px-8 py-6 text-sm text-encre-muted">Chargement des espaces…</p>
      )}

      {!isLoading && isError && (
        <p className="px-8 py-6 text-sm text-erreur">
          Impossible de charger les espaces de cours.
        </p>
      )}

      {!isLoading && !isError && (!all || all.length === 0) && (
        <p className="px-8 py-6 text-sm text-encre-muted">Aucun espace créé pour l'instant.</p>
      )}

      {!isLoading && !isError && all && all.length > 0 && (
        <div>
          <EtagereFiltres
            value={value}
            onChange={setValue}
            tags={extraireTags(all ?? [])}
            showOwnerFilter
          />
          {value.owner !== 'autres' && (
            <EtagereSection
              titre="Mes espaces de cours"
              description="Ceux dont tu es le propriétaire"
              espaces={miens}
              emptyMessage="Aucun espace à toi pour l'instant — crée le premier avec le bouton ci-dessus."
              basePath="enseignant"
            />
          )}
          {value.owner !== 'miens' && (
            <EtagereSection
              titre="Autres espaces (supervision)"
              description="Espaces des autres enseignants, en lecture seule"
              espaces={autres}
              emptyMessage="Aucun autre espace à superviser pour l'instant."
              basePath="enseignant"
            />
          )}
        </div>
      )}
    </div>
  );
}
