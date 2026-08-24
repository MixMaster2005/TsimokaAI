import { useState, type FormEvent } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export const Route = createFileRoute('/_public/mot-de-passe-oublie')({
  component: MotDePasseOublie,
});

/**
 * TODO : pas d'endpoint dédié repéré côté user-service au moment du
 * scaffolding (seuls /register, /login, /refresh existaient) — à ajouter
 * côté back (ex: POST /api/v1/auth/forgot-password) avant de brancher
 * un vrai hook features/auth/api/use-forgot-password.ts ici.
 */
function MotDePasseOublie() {
  const [envoye, setEnvoye] = useState(false);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setEnvoye(true);
  }

  if (envoye) {
    return (
      <p className="max-w-sm text-center text-sm text-encre-muted">
        Si un compte existe avec cet email, un lien de réinitialisation vient d'être envoyé.
      </p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4">
      <h2 className="font-display text-xl font-semibold text-encre">Mot de passe oublié</h2>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" required />
      </div>
      <Button type="submit">Envoyer le lien</Button>
    </form>
  );
}
