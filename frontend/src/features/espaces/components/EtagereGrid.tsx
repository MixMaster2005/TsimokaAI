import { useState } from 'react';

import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
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
  onRetry?: () => void;
  /** Contrôlé (URL) quand fourni, sinon état local. */
  filtres?: FiltresEtagere;
  onFiltresChange?: (v: FiltresEtagere) => void;
}

export function EtagereGrid({ espaces, isLoading, isError, currentUserId, onRetry, filtres, onFiltresChange }: EtagereGridProps) {
  const [valueLocal, setValueLocal] = useState<FiltresEtagere>(FILTRES_ETAGERE_DEFAUT);
  const value = filtres ?? valueLocal;
  const setValue = onFiltresChange ?? setValueLocal;

  if (isLoading) {
    return (
      <div role="status" aria-live="polite" className="flex flex-col gap-3 px-4 py-6 sm:px-6 lg:px-8" aria-busy="true">
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-2/3" />
        <span className="sr-only">Chargement des espaces…</span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-start gap-3 px-4 py-6 sm:px-6 lg:px-8">
        <p role="alert" className="text-sm text-erreur">
          Impossible de charger tes espaces. Vérifie ta connexion puis réessaie.
        </p>
        {onRetry && (
          <Button variant="outline" size="sm" onClick={onRetry}>
            Réessayer
          </Button>
        )}
      </div>
    );
  }

  if (!espaces || espaces.length === 0) {
    return (
      <p className="px-4 py-6 text-sm sm:px-6 lg:px-8 text-encre-muted">
        Aucun espace pour l'instant — crée le premier avec le bouton ci-dessus.
      </p>
    );
  }

  const espacesFiltres = filtrerEspaces(espaces, value, currentUserId);
  const estMien = (s: Space) =>
    s.owner === true || (currentUserId !== undefined && s.userId === currentUserId);
  const miens = espacesFiltres.filter((s) => estMien(s));
  const rejoint = espacesFiltres.filter((s) => !estMien(s));

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
