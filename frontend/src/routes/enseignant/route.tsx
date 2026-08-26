import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';

import { AppSidebarEnseignant } from '@/components/shared/AppSidebarEnseignant';
import { sessionQueryOptions } from '@/features/auth/api/use-session';

/**
 * `enseignant` = layout PRÉFIXÉ du rôle ADMIN (enseignant), distinct de `_app`
 * (étudiant). Préfixé volontairement (pas pathless sans `_`) : deux layouts
 * pathless produiraient des chemins complets identiques et un conflit au
 * générateur de routes, comme pour le compromis `/` vs `/accueil`.
 *
 * Le guard est BILATÉRAL :
 *   - ici, un non-ADMIN repart vers l'app étudiant (`/`) ;
 *   - dans `_app`, un ADMIN est redirigé vers `/enseignant`.
 */
export const Route = createFileRoute('/enseignant')({
  beforeLoad: async ({ context: { queryClient } }) => {
    let session;
    try {
      session = await queryClient.ensureQueryData(sessionQueryOptions);
    } catch {
      throw redirect({ to: '/connexion' });
    }
    if (session.role !== 'ADMIN') {
      throw redirect({ to: '/' });
    }
  },
  component: EnseignantLayout,
});

function EnseignantLayout() {
  return (
    <div className="flex min-h-screen">
      <AppSidebarEnseignant />
      <main className="min-w-0 flex-1 bg-background text-foreground">
        <Outlet />
      </main>
    </div>
  );
}
