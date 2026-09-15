import { createFileRoute, useParams } from '@tanstack/react-router';

import { conversationsQueryOptions } from '@/features/chat/api/use-conversations';
import { ChatPage } from '@/components/shared/ChatPage';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/chat')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(conversationsQueryOptions(params.spaceId)),
  component: ChatEnseignant,
});

function ChatEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/chat' });
  return <ChatPage spaceId={spaceId} showSpaceBar mode="enseignant" />;
}
