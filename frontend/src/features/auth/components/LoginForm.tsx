import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useSearch } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useLogin } from '../api/use-login';

export function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const search = useSearch({ from: '/_public/connexion' });
  // Garde anti open-redirect : chemin interne uniquement (`//evil.com`
  // passe `startsWith('/')` mais bascule vers un domaine hostile).
  const redirectTo =
    typeof search.redirect === 'string' &&
    search.redirect.startsWith('/') &&
    !search.redirect.startsWith('//')
      ? search.redirect
      : undefined;
  const login = useLogin(redirectTo ? { redirectTo } : undefined);
  const erreurRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (login.isError) erreurRef.current?.focus();
  }, [login.isError]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    login.mutate({ email, password });
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4" noValidate={false}>
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
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={login.isError}
          aria-describedby={login.isError ? 'erreur-connexion' : undefined}
        />
      </div>

      {login.isError && (
        <p id="erreur-connexion" ref={erreurRef} tabIndex={-1} role="alert" className="text-sm text-erreur focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          {/* ApiError.message vient directement de l'enveloppe { error: { message } } du back */}
          {login.error.message} — vérifie ton email et ton mot de passe, puis réessaie.
        </p>
      )}

      <Button type="submit" disabled={login.isPending}>
        {login.isPending ? 'Connexion…' : 'Se connecter'}
      </Button>

      <div className="flex justify-between text-xs text-muted-foreground">
        <Link to="/mot-de-passe-oublie" className="hover:text-foreground">
          Mot de passe oublié ?
        </Link>
        <Link to="/inscription" className="hover:text-foreground">
          Créer un compte
        </Link>
      </div>
    </form>
  );
}
