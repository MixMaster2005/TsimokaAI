import { createFileRoute, useParams } from '@tanstack/react-router';

import { documentsBySpaceQueryOptions } from '@/features/documents/api/use-documents';
import { DocumentsPage } from '@/components/shared/DocumentsPage';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/documents')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(documentsBySpaceQueryOptions(params.spaceId)),
  component: DocumentsEnseignant,
});

function DocumentsEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/documents' });
  return <DocumentsPage spaceId={spaceId} />;
}
