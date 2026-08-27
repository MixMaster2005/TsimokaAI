import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

import { LoginForm } from '@/features/auth/components/LoginForm';

/**
 * Search params : redirect = URL d'origine à restaurer après connexion.
 * Le guard `_app/route.tsx` passe l'URL courante via ce param.
 */
const connexionSearchSchema = z.object({
  redirect: z.string().optional(),
});

export const Route = createFileRoute('/_public/connexion')({
  validateSearch: connexionSearchSchema,
  component: () => (
    <div className="flex flex-col items-center gap-6">
      <h2 className="font-display text-xl font-semibold text-encre">Connexion</h2>
      <LoginForm />
    </div>
  ),
});
