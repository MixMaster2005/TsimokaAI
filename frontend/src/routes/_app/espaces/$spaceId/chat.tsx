import { createFileRoute, useParams } from '@tanstack/react-router';

import { conversationsQueryOptions } from '@/features/chat/api/use-conversations';
import { ChatPage } from '@/components/shared/ChatPage';

export const Route = createFileRoute('/_app/espaces/$spaceId/chat')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(conversationsQueryOptions(params.spaceId)),
  component: Chat,
});

function Chat() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/chat' });
  return <ChatPage spaceId={spaceId} showSpaceBar />;
}
