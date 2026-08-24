import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';

import { AppSidebar } from '@/components/shared/AppSidebar';
import { sessionQueryOptions } from '@/features/auth/api/use-session';

/**
 * `_app` = pathless layout, enveloppe tout ce qui est authentifié.
 * `beforeLoad` s'exécute AVANT le rendu de n'importe quelle route enfant —
 * c'est le seul et unique endroit qui doit vérifier l'auth pour tout ce
 * qui vit sous ce layout (étagère, dashboard, espaces…). Pas de vérif
 * dupliquée route par route.
 *
 * ⚠️ Cette route sert la variante STUDENT. La variante ADMIN (enseignant)
 * a sa propre coquille (nav totalement différente, cf. cartographie UI C.2) —
 * à créer en `_app-enseignant` séparé plutôt que de brancher un `if role`
 * ici, pour ne pas transformer ce fichier en fourre-tout conditionnel.
 * Le `beforeLoad` ci-dessous devra alors aussi rediriger un ADMIN qui
 * atterrit ici vers son propre layout.
 */
export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context: { queryClient } }) => {
    try {
      await queryClient.ensureQueryData(sessionQueryOptions);
    } catch {
      // TODO : mémoriser location.href (ex: dans un search param typé via
      // validateSearch sur /_public/connexion) pour rediriger l'utilisateur
      // vers sa page d'origine après connexion, plutôt que systématiquement /.
      // throw redirect({ to: '/connexion' });
    }
  },
  component: AppLayout,
});

function AppLayout() {
  return (
    <div className="flex min-h-screen">
      <AppSidebar />
      <main className="min-w-0 flex-1 bg-background text-foreground">
        <Outlet />
      </main>
    </div>
  );
}
