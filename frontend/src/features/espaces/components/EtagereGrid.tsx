import { useState } from 'react';

import { EtagereFiltres } from './EtagereFiltres';
import { EtagereSection } from './EtagereSection';
import {
  FILTRES_ETAGERE_DEFAUT,
  extraireTags,
  filtrerEspaces,
  type FiltresEtagere,
} from '../lib/filtrer-espaces';
import type { Space } from '../types';

interface EtagereGridProps {
  espaces: Space[];
  isLoading: boolean;
  isError: boolean;
  currentUserId?: string;
}

export function EtagereGrid({ espaces, isLoading, isError, currentUserId }: EtagereGridProps) {
  const [value, setValue] = useState<FiltresEtagere>(FILTRES_ETAGERE_DEFAUT);

  if (isLoading) {
    return <p className="px-8 py-6 text-sm text-encre-muted">Chargement des espaces…</p>;
  }

  if (isError) {
    return <p className="px-8 py-6 text-sm text-erreur">Impossible de charger tes espaces.</p>;
  }

  if (!espaces || espaces.length === 0) {
    return (
      <p className="px-8 py-6 text-sm text-encre-muted">
        Aucun espace pour l'instant — crée le premier avec le bouton ci-dessus.
      </p>
    );
  }

  const filtres = filtrerEspaces(espaces, value, currentUserId);
  const estMien = (s: Space) =>
    s.owner === true || (currentUserId !== undefined && s.userId === currentUserId);
  const miens = filtres.filter((s) => estMien(s));
  const rejoint = filtres.filter((s) => !estMien(s));

  return (
    <div>
      <EtagereFiltres
        value={value}
        onChange={setValue}
        tags={extraireTags(espaces)}
        showOwnerFilter
      />
      {value.owner !== 'autres' && (
        <EtagereSection
          titre="Mes espaces"
          description="Ceux dont tu es le propriétaire"
          espaces={miens}
          emptyMessage="Aucun espace à toi pour l'instant — crée le premier avec le bouton ci-dessus."
          basePath="etudiant"
        />
      )}
      {value.owner !== 'miens' && (
        <EtagereSection
          titre="Espaces rejoints"
          description="Partagés via code d'invitation"
          espaces={rejoint}
          emptyMessage="Aucun espace rejoint pour l'instant — rejoins-en un avec un code d'invitation."
          basePath="etudiant"
        />
      )}
    </div>
  );
}

export default EtagereGrid;
