import { createFileRoute, redirect } from '@tanstack/react-router';

/**
 * /espaces/$spaceId seul (sans sous-onglet) atterrit directement sur Chat —
 * c'est la réponse concrète à "le Chat est-il noyé ?" : cliquer un espace
 * depuis l'étagère amène DIRECTEMENT sur le chat, zéro clic supplémentaire.
 */
export const Route = createFileRoute('/_app/espaces/$spaceId/')({
  beforeLoad: ({ params }) => {
    throw redirect({ to: '/espaces/$spaceId/chat', params: { spaceId: params.spaceId } });
  },
});
