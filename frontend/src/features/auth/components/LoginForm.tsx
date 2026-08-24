import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useLogin } from '../api/use-login';

export function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const login = useLogin();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    login.mutate({ email, password });
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="password">Mot de passe</Label>
        <Input
          id="password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      {login.isError && (
        <p className="text-sm text-erreur">
          {/* ApiError.message vient directement de l'enveloppe { error: { message } } du back */}
          {login.error.message}
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
