import { createFileRoute, useParams } from '@tanstack/react-router';

import { documentsBySpaceQueryOptions } from '@/features/documents/api/use-documents';
import { DocumentsPage } from '@/components/shared/DocumentsPage';

export const Route = createFileRoute('/_app/espaces/$spaceId/documents')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(documentsBySpaceQueryOptions(params.spaceId)),
  component: Documents,
});

function Documents() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/documents' });
  return <DocumentsPage spaceId={spaceId} />;
}
