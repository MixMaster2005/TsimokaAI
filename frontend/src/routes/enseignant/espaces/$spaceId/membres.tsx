import { createFileRoute, useParams } from '@tanstack/react-router';

import { membresQueryOptions } from '@/features/espaces/api/use-membres';
import { groupesBySpaceQueryOptions } from '@/features/groupes/api/keys';
import { MembresPageEnseignant } from '@/components/shared/MembresPageEnseignant';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/membres')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(groupesBySpaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(membresQueryOptions(params.spaceId)),
    ]),
  component: MembresEnseignant,
});

function MembresEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/membres' });
  return <MembresPageEnseignant spaceId={spaceId} basePath="/enseignant" />;
}
