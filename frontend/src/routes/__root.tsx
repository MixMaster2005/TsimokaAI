import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import type { QueryClient } from "@tanstack/react-query";

interface RouterContext {
  queryClient: QueryClient;
}

/**
 * __root.tsx est LA seule route sans URL propre — elle enveloppe TOUT
 * l'arbre (_public, onboarding, _app...). C'est ici et UNIQUEMENT ici
 * qu'on met ce qui doit survivre à chaque navigation : devtools, error
 * boundary global, éventuel <Toaster /> pour les notifications globales.
 *
 * Rappel du contrat de design : cette route ne doit jamais imposer de
 * surface (Ardoise/Papier) — chaque sous-arbre (_public, _app, l'espace
 * en Ardoise) décide de la sienne. __root reste neutre.
 *
 * L'initialisation auth est gérée naturellement par TanStack Query :
 * sessionQueryOptions se lance au premier ensureQueryData() du guard,
 * l'interceptor 401 de apiClient tente le refresh silencieux si besoin.
 */
export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootComponent,
});

function RootComponent() {
  return (
    <>
      <Outlet />
      {import.meta.env.DEV && (
        <>
          <TanStackRouterDevtools position="bottom-right" />
          <ReactQueryDevtools
            initialIsOpen={false}
            buttonPosition="bottom-left"
          />
        </>
      )}
    </>
  );
}
