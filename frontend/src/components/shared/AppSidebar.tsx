import { Link, useNavigate } from '@tanstack/react-router';
import { Bell, LayoutGrid, Sparkles, Files, Home } from 'lucide-react';

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
import { useRappels } from '@/features/gamification/api/use-rappels';
import { setAccessToken } from '@/lib/api-client';

/**
 * Chrome de navigation persistant du Layout App — Étudiant.
 * Volontairement toujours en surface Ardoise (voir la discussion du 23/08 :
 * un rail de navigation persistant n'est ni "contenu Papier" ni "chat", donc
 * on l'a tranché en chrome sombre permanent — cohérent avec le chat quand il
 * est actif, contraste normal avec les pages Papier sinon, comme Notion/Linear).
 *
 * ⚠️ Existe en deux variantes : celle-ci pour STUDENT. Une AppSidebarEnseignant
 * séparée est à écrire pour ADMIN (nav différente — cf. cartographie UI C.2),
 * pas un simple `if (role)` dans ce fichier : les deux rôles n'ont presque
 * aucun item de nav en commun, autant garder deux composants lisibles.
 */
const NAV_ITEMS = [
  { to: '/', label: 'Mes espaces', icon: Home },
  { to: '/tableau-de-bord', label: 'Tableau de bord', icon: LayoutGrid },
  { to: '/objectifs', label: 'Objectifs & badges', icon: Sparkles },
] as const;

const NAV_SECONDAIRE = [{ to: '/mes-fiches', label: 'Mes fiches', icon: Files }] as const;

export function AppSidebar() {
  const { data: user } = useSession();
  const { data: rappels } = useRappels();
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
      <Link to="/" className="flex items-center gap-2 px-4 py-5 font-display text-base font-semibold">
        🌱 TsimokaAI
      </Link>

      <nav className="flex flex-col gap-1 px-2">
        <p className="px-2 pb-1 pt-3 font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">
          Principal
        </p>
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.to} {...item} />
        ))}

        <p className="px-2 pb-1 pt-3 font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">
          Secondaire
        </p>
        {NAV_SECONDAIRE.map((item) => (
          <NavLink key={item.to} {...item} />
        ))}
      </nav>

      <div className="mt-auto flex items-center gap-2 border-t border-border p-3">
        <Avatar>
          <AvatarFallback>{initials ?? '…'}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{user?.displayName ?? '…'}</p>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger className="relative rounded-md p-1.5 hover:bg-secondary" aria-label="Rappels">
            <Bell className="size-4" />
            {rappels && rappels.filter((r) => !r.envoye).length > 0 && (
              <span className="absolute right-1 top-1 size-1.5 rounded-full bg-attention" />
            )}
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Rappels récents</DropdownMenuLabel>
            {!rappels || rappels.length === 0 ? (
              <DropdownMenuItem disabled>Aucun rappel pour l\u2019instant</DropdownMenuItem>
            ) : (
              rappels.slice(0, 5).map((r) => (
                <DropdownMenuItem key={r.id} disabled className="flex-col items-start whitespace-normal">
                  <span>{r.message}</span>
                  <span className="font-mono text-[0.62rem] text-muted-foreground">
                    {new Date(r.prevuLe).toLocaleDateString('fr-FR')}
                  </span>
                </DropdownMenuItem>
              ))
            )}
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link to="/parametres">Paramètres</Link>
            </DropdownMenuItem>
            <DropdownMenuItem onClick={handleLogout}>Déconnexion</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </aside>
  );
}

function NavLink({ to, label, icon: Icon }: { to: string; label: string; icon: typeof Home }) {
  return (
    <Link
      to={to}
      activeOptions={{ exact: to === '/' }}
      className="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
      activeProps={{ className: 'bg-secondary text-foreground' }}
    >
      <Icon className="size-4" />
      {label}
    </Link>
  );
}
