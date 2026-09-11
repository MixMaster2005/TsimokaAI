import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/')({
  beforeLoad: ({ params }) => {
    throw redirect({ to: '/enseignant/espaces/$spaceId/fiches', params: { spaceId: params.spaceId } });
  },
});
