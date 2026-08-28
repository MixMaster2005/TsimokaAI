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
 * Cette route sert la variante STUDENT. La variante ENSEIGNANT vit
 * sous `routes/enseignant/` (layout + sidebar dédiés) et les deux guards sont
 * bilatéraux : un ENSEIGNANT qui atterrit ici repart vers /enseignant, un
 * non-ENSEIGNANT qui atterrit sous /enseignant repart vers /
 */
export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context: { queryClient }, location }) => {
    let session;
    try {
      session = await queryClient.ensureQueryData(sessionQueryOptions);
    } catch {
      // La landing reste accessible via son URL explicite. La racine est
      // néanmoins accueillante pour un visiteur qui arrive directement sur
      // le domaine, sans casser le point d'entrée historique de l'app pour
      // un étudiant déjà connecté.
      if (location.pathname === '/') {
        throw redirect({ to: '/accueil' });
      }

      throw redirect({
        to: '/connexion',
        // `search` est l'objet de paramètres analysé par TanStack Router ;
        // `href` contient déjà pathname + query string + hash sous forme de
        // chaîne et peut donc être restauré après la connexion.
        search: { redirect: location.href },
      });
    }
    if (session.role === 'ENSEIGNANT') {
      throw redirect({ to: '/enseignant' });
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
