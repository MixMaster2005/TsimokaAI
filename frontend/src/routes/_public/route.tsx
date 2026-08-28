import { createFileRoute, Link, Outlet } from '@tanstack/react-router';

/**
 * Le préfixe `_` (dossier _public) = route "pathless" chez TanStack Router :
 * elle enveloppe ses enfants (index, connexion, inscription...) sans
 * ajouter de segment à l'URL. /connexion reste /connexion, pas /_public/connexion.
 *
 * Toujours en surface Papier (jamais Ardoise ici — voir contrat de design §2).
 */
/**
 * Layout A. ⚠️ Compromis pris pendant le scaffolding : `_app` et `_public`
 * sont tous deux des layouts pathless, donc ils ne peuvent pas revendiquer
 * `/` en même temps (conflit détecté par le build). `/` est laissé à `_app`
 * (l'étagère, protégée par son beforeLoad qui redirige vers /connexion si
 * non connecté) ; la Landing marketing vit donc à `/accueil`, pas à `/`.
 * Ce n'est probablement pas ce qu'on veut pour un vrai lancement public
 * (un visiteur qui tape le nom de domaine devrait voir la Landing, pas un
 * redirect vers /connexion) — à retrancher consciemment : soit accepter ce
 * compromis pour le MVP, soit déplacer l'étagère sous un préfixe /app.
 */
export const Route = createFileRoute('/_public')({
  component: PublicLayout,
});

function PublicLayout() {
  return (
    <div className="min-h-screen bg-papier-bg">
      <header className="flex items-center justify-between px-6 py-4">
        <Link to="/accueil" className="font-display text-lg font-semibold text-encre">
          🌱 TsimokaAI
        </Link>
        <nav className="flex gap-4 text-sm text-encre-muted">
          <Link to="/connexion" className="hover:text-encre">
            Connexion
          </Link>
          <Link to="/inscription" className="hover:text-encre">
            Créer un compte
          </Link>
        </nav>
      </header>
      <main className="flex flex-1 flex-col items-center px-6 py-10">
        <Outlet />
      </main>
      <footer className="border-t border-papier-border px-6 py-6 text-center text-xs text-encre-muted">
        <p>🌱 TsimokaAI &copy; {new Date().getFullYear()} — Tous droits r&eacute;serv&eacute;s</p>
      </footer>
    </div>
  );
}
