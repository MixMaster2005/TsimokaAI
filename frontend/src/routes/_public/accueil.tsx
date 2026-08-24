import { createFileRoute, Link } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';

export const Route = createFileRoute('/_public/accueil')({
  component: Landing,
});

const PILIERS = [
  {
    titre: 'Fiches typées, pas un résumé libre',
    desc: 'Définition, points clés, exemple appliqué — une structure fixe plutôt qu\u2019un texte généré au hasard.',
  },
  {
    titre: 'Un persona par espace, généré automatiquement',
    desc: 'Chaque matière obtient un assistant qui lui ressemble, sans que tu aies à le rédiger toi-même.',
  },
  {
    titre: 'Pensé pour plusieurs utilisateurs',
    desc: 'Un enseignant configure l\u2019espace, les étudiants l\u2019exploitent et collaborent dessus.',
  },
];

function Landing() {
  return (
    <div className="flex max-w-2xl flex-col items-center gap-8 text-center">
      <div>
        <h1 className="font-display text-4xl font-semibold text-encre">
          Transforme tes cours en fiches qui tiennent debout.
        </h1>
        <p className="mt-3 text-encre-muted">
          Dépose tes documents, discute avec un assistant qui connaît ton cours, garde des fiches de révision
          structurées.
        </p>
      </div>

      <div className="flex gap-3">
        <Button size="lg" asChild>
          <Link to="/inscription">Créer un compte</Link>
        </Button>
        <Button size="lg" variant="outline" asChild>
          <Link to="/connexion">Se connecter</Link>
        </Button>
      </div>

      <div className="grid gap-4 text-left sm:grid-cols-3">
        {PILIERS.map((p) => (
          <div key={p.titre} className="rounded-lg border border-papier-border bg-papier-carte p-4">
            <h3 className="font-display text-sm font-semibold text-encre">{p.titre}</h3>
            <p className="mt-1 text-xs text-encre-muted">{p.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
