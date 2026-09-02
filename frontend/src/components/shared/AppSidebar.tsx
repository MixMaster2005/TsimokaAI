import { Link, useLocation, useNavigate } from '@tanstack/react-router';
import { Bell, LayoutGrid, Sparkles, Files, Home, UserPlus } from 'lucide-react';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar';
import { useSession } from '@/features/auth/api/use-session';
import { useRappels } from '@/features/gamification/api/use-rappels';
import { JoinEspaceModal } from '@/features/espaces/components/JoinEspaceModal';
import { clearTokens } from '@/lib/auth-tokens';

/**
 * Chrome de navigation persistant du Layout App — Étudiant.
 * Construit sur le composant Sidebar de shadcn/ui, rétractable via le rail
 * (ou le raccourci Ctrl/Cmd+B). La hauteur est fixe sur l'écran (h-svh), seul
 * le contenu de navigation scrolle (SidebarContent).
 *
 * ⚠️ Existe en deux variantes : celle-ci pour STUDENT. Une AppSidebarEnseignant
 * séparée est à écrire pour ENSEIGNANT (nav différente — cf. cartographie UI C.2),
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
  const { state } = useSidebar();

  return (
    <Sidebar collapsible="icon" className="surface-ardoise">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild size="lg" tooltip="TsimokaAI">
              <Link to="/" className="font-display text-base font-semibold text-craie">
                <span aria-hidden>🌱</span>
                {state === 'expanded' && <span>TsimokaAI</span>}
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel className="font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
            Principal
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {NAV_ITEMS.map((item) => (
                <NavItem key={item.to} to={item.to} label={item.label} icon={item.icon} />
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel className="font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
            Secondaire
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {NAV_SECONDAIRE.map((item) => (
                <NavItem key={item.to} to={item.to} label={item.label} icon={item.icon} />
              ))}
              <SidebarMenuItem>
                <JoinEspaceModal
                  trigger={
                    <SidebarMenuButton asChild tooltip="Rejoindre un espace">
                      <button type="button" className="text-craie">
                        <UserPlus />
                        <span>Rejoindre un espace</span>
                      </button>
                    </SidebarMenuButton>
                  }
                />
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <UserMenu />
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}

function NavItem({ to, label, icon: Icon }: { to: string; label: string; icon: typeof Home }) {
  const { state } = useSidebar();
  const { pathname } = useLocation();
  const isActive = to === '/' ? pathname === '/' : pathname.startsWith(to);

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        asChild
        isActive={isActive}
        tooltip={state === 'collapsed' ? label : undefined}
      >
        <Link to={to} activeOptions={{ exact: to === '/' }} className="text-craie">
          <Icon />
          <span>{label}</span>
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}

function UserMenu() {
  const { data: user } = useSession();
  const { data: rappels } = useRappels();
  const navigate = useNavigate();

  function handleLogout() {
    clearTokens();
    navigate({ to: '/connexion' });
  }

  const initials = user?.displayName
    ?.split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <SidebarMenuButton size="lg" tooltip={user?.displayName ?? '…'}>
          <Avatar className="size-5">
            <AvatarFallback className="text-[0.625rem]">{initials ?? '…'}</AvatarFallback>
          </Avatar>
          <span className="truncate text-craie">{user?.displayName ?? '…'}</span>
          <span className="ml-auto flex items-center">
            <span className="relative inline-flex">
              <Bell className="size-4" />
              {rappels && rappels.filter((r) => !r.envoye).length > 0 && (
                <span className="absolute right-1 top-1 size-1.5 rounded-full bg-attention" />
              )}
            </span>
          </span>
        </SidebarMenuButton>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" side="top">
        <DropdownMenuLabel>Rappels récents</DropdownMenuLabel>
        {!rappels || rappels.length === 0 ? (
          <DropdownMenuItem disabled>Aucun rappel pour l'instant</DropdownMenuItem>
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
  );
}
