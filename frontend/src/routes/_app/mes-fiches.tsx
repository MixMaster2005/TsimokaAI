import { createFileRoute } from '@tanstack/react-router';

/**
 * ⚠️ BLOQUANT, pas juste "pas encore fait" : FicheController n'expose que
 * `GET /api/v1/fiches?spaceId=` — spaceId est un @RequestParam UUID
 * *obligatoire* (pas de valeur par défaut, pas de endpoint "toutes mes
 * fiches tous espaces confondus"). Cette page telle que décrite dans la
 * cartographie UI n'est donc PAS implémentable sans un des deux ajouts
 * côté back :
 *   (a) rendre spaceId optionnel dans FicheController.listMine et filtrer
 *       par userId (déjà dans le JWT/header X-User-Id) quand absent
 *   (b) exposer un endpoint dédié GET /api/v1/fiches/mine
 *
 * C'est exactement le point ouvert de la cartographie UI ("nécessaire dès
 * le MVP ou reportable ?") — sauf que maintenant on sait que la réponse a
 * un coût back, pas juste front. Vaut le coup d'en reparler avant de trancher.
 *
 * En attendant, cette page reste un guard visuel plutôt qu'un vrai fetch.
 */
export const Route = createFileRoute('/_app/mes-fiches')({
  component: MesFiches,
});

function MesFiches() {
  return (
    <div className="p-8">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Vue transverse</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Mes fiches</h1>
      <div className="rounded-fiche border border-dashed border-papier-border bg-papier-carte p-6 text-sm text-encre-muted">
        Nécessite un endpoint backend qui n\u2019existe pas encore — voir le commentaire en tête de ce fichier.
      </div>
    </div>
  );
}
