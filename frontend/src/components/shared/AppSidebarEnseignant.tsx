import { Link, useNavigate } from '@tanstack/react-router';
import { GraduationCap, LayoutGrid } from 'lucide-react';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useSession } from '@/features/auth/api/use-session';
import { setAccessToken } from '@/lib/api-client';

/**
 * Chrome de navigation du Layout App — ENSEIGNANT (rôle ADMIN).
 *
 * Composant séparé d'AppSidebar plutôt qu'un `if (role)` : les deux rôles n'ont
 * aucun item de navigation en commun et le contrat de design décrit un usage
 * enseignant ponctuel (configuration, supervision), sans instrumentation
 * quotidienne.
 *
 * Surface Ardoise permanente : même règle que la sidebar étudiant — un rail de
 * navigation persistant n'est ni "contenu Papier" ni "chat".
 */
const NAV_ITEMS = [
  { to: '/enseignant', label: 'Tableau de bord', icon: LayoutGrid },
] as const;

export function AppSidebarEnseignant() {
  const { data: user } = useSession();
  const navigate = useNavigate();

  function handleLogout() {
    setAccessToken(null);
    navigate({ to: '/connexion' });
  }

  const initials = user?.displayName
    ?.split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <aside className="surface-ardoise flex w-[250px] flex-none flex-col border-r border-border bg-background text-foreground">
      <Link to="/enseignant" className="flex items-center gap-2 px-4 py-5 font-display text-base font-semibold">
        🌱 TsimokaAI
        <GraduationCap className="ml-auto h-4 w-4 text-muted-foreground" aria-hidden />
      </Link>

      <nav className="flex flex-col gap-1 px-2">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} label={label} icon={Icon} />
        ))}
      </nav>

      <div className="mt-auto border-t border-border p-3">
        <DropdownMenu>
          <DropdownMenuTrigger className="flex w-full items-center gap-2 rounded-md p-2 text-left hover:bg-secondary">
            <Avatar className="h-7 w-7">
              <AvatarFallback className="text-xs">{initials}</AvatarFallback>
            </Avatar>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm">{user?.displayName ?? '…'}</p>
              <p className="font-mono text-[0.62rem] uppercase tracking-wide text-muted-foreground">Enseignant</p>
            </div>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" side="top">
            <DropdownMenuLabel>{user?.email}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {/* Le rôle ADMIN n'a pas de fiche profil étudiant à éditer ici :
                pas de Paramètres pour l'instant, seulement la déconnexion. */}
            <DropdownMenuItem onClick={handleLogout}>Déconnexion</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </aside>
  );
}

function NavLink({
  to,
  label,
  icon: Icon,
}: {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <Link
      to={to}
      activeOptions={{ exact: true }}
      activeProps={{ className: 'bg-secondary text-foreground' }}
      className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground hover:text-foreground"
    >
      <Icon className="h-4 w-4" />
      {label}
    </Link>
  );
}
