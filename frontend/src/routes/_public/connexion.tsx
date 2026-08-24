import { createFileRoute } from '@tanstack/react-router';

import { LoginForm } from '@/features/auth/components/LoginForm';

export const Route = createFileRoute('/_public/connexion')({
  component: () => (
    <div className="flex flex-col items-center gap-6">
      <h2 className="font-display text-xl font-semibold text-encre">Connexion</h2>
      <LoginForm />
    </div>
  ),
});
