import { useEffect, useRef } from 'react';

import { ChatInput } from './ChatInput';
import { ChatMessage } from './ChatMessage';
import { useMessages } from '../api/use-messages';
import { useSendMessage } from '../api/use-send-message';

interface ChatThreadProps {
  conversationId: string;
  spaceId: string;
}

export function ChatThread({ conversationId, spaceId }: ChatThreadProps) {
  const { data: messages, isLoading } = useMessages(conversationId);
  const sendMessage = useSendMessage(conversationId, spaceId);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages?.length]);

  return (
    <div className="flex h-full flex-col">
      <div className="mx-auto w-full max-w-2xl flex-1 overflow-y-auto px-6 py-6">
        {isLoading && <p className="text-sm text-muted-foreground">Chargement de la conversation…</p>}
        {messages?.map((message, i) => (
          <ChatMessage
            key={message.id}
            message={message}
            // N'anime que le tout dernier message si on vient de l'envoyer (mutation pas encore "settled")
            animate={i === messages.length - 1 && sendMessage.isPending === false && sendMessage.isSuccess}
          />
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="mx-auto w-full max-w-2xl px-6 pb-6">
        <ChatInput
          onSend={(content) => sendMessage.mutate({ content })}
          disabled={sendMessage.isPending}
        />
      </div>
    </div>
  );
}
