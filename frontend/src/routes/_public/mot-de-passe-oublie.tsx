import { useState, type FormEvent } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useForgotPassword } from '@/features/auth/api/use-forgot-password';

export const Route = createFileRoute('/_public/mot-de-passe-oublie')({
  component: MotDePasseOublie,
});

function MotDePasseOublie() {
  const [email, setEmail] = useState('');
  const [envoye, setEnvoye] = useState(false);
  const forgotPassword = useForgotPassword();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!email.trim()) return;
    forgotPassword.mutate(
      { email: email.trim() },
      {
        onSettled: () => {
          setEnvoye(true);
        },
      },
    );
  }

  if (envoye) {
    return (
      <div className="flex max-w-sm flex-col items-center gap-4 text-center">
        <h2 className="font-display text-xl font-semibold text-encre">Demande envoyée</h2>
        <p className="text-sm text-encre-muted">
          Si un compte existe avec cet email ({email}), un lien de réinitialisation vous sera envoyé dès que le service d'envoi sera actif.
        </p>
        <p className="rounded-md border border-dashed border-papier-border bg-papier-carte p-3 font-mono text-xs text-attention">
          ℹ️ Note : La réinitialisation automatique de mot de passe sera pleinement disponible prochainement.
        </p>
        <Button variant="outline" asChild className="mt-2">
          <Link to="/connexion">Retour à la connexion</Link>
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4">
      <h2 className="font-display text-xl font-semibold text-encre">Mot de passe oublié</h2>
      <p className="text-xs text-encre-muted">
        Entrez votre adresse email pour recevoir les instructions de réinitialisation.
      </p>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoFocus
        />
      </div>
      <Button type="submit" disabled={forgotPassword.isPending || !email.trim()}>
        {forgotPassword.isPending ? 'Envoi en cours…' : 'Envoyer le lien'}
      </Button>
      <div className="text-center">
        <Link to="/connexion" className="text-xs text-encre-muted hover:text-encre">
          ← Retour à la connexion
        </Link>
      </div>
    </form>
  );
}
