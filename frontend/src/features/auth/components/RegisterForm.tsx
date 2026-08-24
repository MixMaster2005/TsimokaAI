import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useRegister } from '../api/use-register';

/**
 * ⚠️ Point ouvert (cf. cartographie UI, Layout Public) : ce formulaire
 * n'expose PAS de choix de rôle STUDENT/ADMIN — l'inscription crée
 * toujours un STUDENT par défaut (cohérent avec RegisterRequest côté back
 * qui rend `role` optionnel). Si vous tranchez pour un choix explicite à
 * l'inscription, ajoutez un <select> ici et passez `role` dans le payload.
 */
export function RegisterForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const register = useRegister();

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
          required
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
        />
      </div>
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
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      {register.isError && <p className="text-sm text-erreur">{register.error.message}</p>}

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
