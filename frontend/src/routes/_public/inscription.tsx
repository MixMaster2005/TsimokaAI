import { createFileRoute } from '@tanstack/react-router';

import { RegisterForm } from '@/features/auth/components/RegisterForm';

export const Route = createFileRoute('/_public/inscription')({
  component: () => (
    <div className="flex flex-col items-center gap-6">
      <h2 className="font-display text-xl font-semibold text-encre">Créer un compte</h2>
      <RegisterForm />
    </div>
  ),
});
