import { useState, type FormEvent } from 'react';

interface ChatInputProps {
  onSend: (content: string) => void;
  disabled?: boolean;
}

export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [value, setValue] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setValue('');
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-busy={disabled}
      className="flex items-center gap-2 rounded-md border border-border bg-secondary py-1.5 pl-4 pr-1.5 focus-within:ring-2 focus-within:ring-ring"
    >
      <label htmlFor="chat-input" className="sr-only">
        Écris ta question sur le cours…
      </label>
      <input
        id="chat-input"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Écris ta question sur le cours…"
        disabled={disabled}
        aria-label="Écris ta question sur le cours…"
        className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none"
      />
      <button
        type="submit"
        disabled={disabled}
        className="rounded-sm bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
      >
        Envoyer
      </button>
    </form>
  );
}
