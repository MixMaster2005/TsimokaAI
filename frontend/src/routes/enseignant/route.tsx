import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';

import { AppSidebarEnseignant } from '@/components/shared/AppSidebarEnseignant';
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar';
import { sessionQueryOptions } from '@/features/auth/api/use-session';

/**
 * `enseignant` = layout PRÉFIXÉ du rôle ENSEIGNANT, distinct de `_app`
 * (étudiant). Préfixé volontairement (pas pathless sans `_`) : deux layouts
 * pathless produiraient des chemins complets identiques et un conflit au
 * générateur de routes, comme pour le compromis `/` vs `/accueil`.
 *
 * Le guard est BILATÉRAL :
 *   - ici, un non-ENSEIGNANT repart vers l'app étudiant (`/`) ;
 *   - dans `_app`, un ENSEIGNANT est redirigé vers `/enseignant`.
 */
export const Route = createFileRoute('/enseignant')({
  beforeLoad: async ({ context: { queryClient } }) => {
    let session;
    try {
      session = await queryClient.ensureQueryData(sessionQueryOptions);
    } catch {
      throw redirect({ to: '/connexion' });
    }
    if (session.role !== 'ENSEIGNANT') {
      throw redirect({ to: '/' });
    }
  },
  component: EnseignantLayout,
});

function EnseignantLayout() {
  return (
    <SidebarProvider>
      <AppSidebarEnseignant />
      <SidebarInset className="min-h-svh bg-background text-foreground">
        <Outlet />
      </SidebarInset>
    </SidebarProvider>
  );
}
