import { createFileRoute, useParams } from '@tanstack/react-router';

import { membresQueryOptions } from '@/features/espaces/api/use-membres';
import { groupesBySpaceQueryOptions } from '@/features/groupes/api/keys';
import { MembresPage } from '@/components/shared/MembresPage';

export const Route = createFileRoute('/_app/espaces/$spaceId/membres')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(groupesBySpaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(membresQueryOptions(params.spaceId)),
    ]),
  component: Membres,
});

function Membres() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/membres' });
  return <MembresPage spaceId={spaceId} basePath="/" />;
}
