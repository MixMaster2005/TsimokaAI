import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useRegister } from '../api/use-register';

/**
 * ⚠️ Point ouvert (cf. cartographie UI, Layout Public) : ce formulaire
 * n'expose PAS de choix de rôle — l'inscription crée
 * toujours un STUDENT par défaut (cohérent avec RegisterRequest côté back
 * qui ne contient pas de champ role). Le choix Étudiant/Enseignant se fait
 * lors de l'onboarding (PATCH /me avec role).
 */
export function RegisterForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const register = useRegister();
  const erreurRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (register.isError) erreurRef.current?.focus();
  }, [register.isError]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    register.mutate({ email, password, displayName });
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="displayName">Nom affiché</Label>
        <Input
          id="displayName"
          name="displayName"
          autoComplete="name"
          required
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          spellCheck={false}
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="password">Mot de passe</Label>
        <Input
          id="password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={register.isError}
          aria-describedby={register.isError ? 'erreur-inscription' : undefined}
        />
      </div>

      {register.isError && (
        <p id="erreur-inscription" ref={erreurRef} tabIndex={-1} role="alert" className="text-sm text-erreur focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          {register.error.message} — corrige et réessaie.
        </p>
      )}

      <Button type="submit" disabled={register.isPending}>
        {register.isPending ? 'Création…' : 'Créer mon compte'}
      </Button>

      <p className="text-center text-xs text-muted-foreground">
        Déjà un compte ?{' '}
        <Link to="/connexion" className="text-foreground hover:underline">
          Se connecter
        </Link>
      </p>
    </form>
  );
}
