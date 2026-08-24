import { SpineCard } from './SpineCard';
import { useEspaces } from '../api/use-espaces';

export function EtagereGrid() {
  const { data: espaces, isLoading, isError } = useEspaces();

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

  return (
    <div className="flex gap-4 overflow-x-auto px-8 py-5">
      {espaces.map((space) => (
        <SpineCard key={space.id} space={space} />
      ))}
    </div>
  );
}
