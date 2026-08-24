import { createFileRoute, Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';

export const Route = createFileRoute('/onboarding/bienvenue')({
  component: Bienvenue,
});

function Bienvenue() {
  return (
    <div className="flex max-w-md flex-col items-center gap-6 text-center">
      <h1 className="font-display text-2xl font-semibold text-encre">Bienvenue sur TsimokaAI</h1>
      <p className="text-sm text-encre-muted">
        Crée ton premier espace de cours, ou rejoins-en un si ton enseignant t\u2019a déjà donné un code.
      </p>
      <div className="flex w-full flex-col gap-2">
        <Button asChild>
          <Link to="/onboarding/creer-espace">Créer mon premier espace</Link>
        </Button>
        <Button variant="outline" disabled>
          {/* TODO : pas de mécanisme de code d'invitation identifié côté back
              au moment du scaffolding — cf. SpaceController, aucune route
              "join by code" repérée. À vérifier avant d'activer ce bouton. */}
          Rejoindre un espace via un code
        </Button>
      </div>
      <Link to="/" className="text-xs text-encre-muted hover:text-encre">
        Passer cette étape
      </Link>
    </div>
  );
}
