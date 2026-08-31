import { Link, useLocation, useNavigate } from '@tanstack/react-router';
import { GraduationCap, Home, Settings } from 'lucide-react';

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
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar';
import { useSession } from '@/features/auth/api/use-session';
import { clearTokens } from '@/lib/auth-tokens';

/**
 * Chrome de navigation du Layout App — ENSEIGNANT.
 *
 * Composant séparé d'AppSidebar plutôt qu'un `if (role)` : les deux rôles n'ont
 * aucun item de navigation en commun et le contrat de design décrit un usage
 * enseignant ponctuel (configuration, supervision), sans instrumentation
 * quotidienne.
 *
 * Construit sur le composant Sidebar de shadcn/ui, rétractable via le rail
 * (ou Ctrl/Cmd+B), hauteur fixe sur l'écran (h-svh), seul le contenu de
 * navigation scrolle (SidebarContent).
 */
const NAV_ITEMS = [
  { to: '/enseignant', label: 'Mes espaces', icon: Home },
  { to: '/enseignant/parametres', label: 'Paramètres', icon: Settings },
] as const;

function NavItem({
  to,
  label,
  icon: Icon,
}: {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}) {
  const { state } = useSidebar();
  const { pathname } = useLocation();
  const isActive = pathname === to || pathname.startsWith(`${to}/`);

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        asChild
        isActive={isActive}
        tooltip={state === 'collapsed' ? label : undefined}
      >
        <Link to={to} activeOptions={{ exact: true }}>
          <Icon />
          <span>{label}</span>
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}

export function AppSidebarEnseignant() {
  const { data: user } = useSession();
  const navigate = useNavigate();
  const { state } = useSidebar();

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
    <Sidebar collapsible="icon" className="surface-ardoise">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild size="lg" tooltip="TsimokaAI">
              <Link to="/enseignant" className="font-display text-base font-semibold">
                <span aria-hidden>🌱</span>
                {state === 'expanded' && (
                  <>
                    <span>TsimokaAI</span>
                    <GraduationCap className="ml-auto text-muted-foreground" aria-hidden />
                  </>
                )}
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {NAV_ITEMS.map((item) => (
                <NavItem key={item.to} {...item} />
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <SidebarMenuButton size="lg" tooltip={user?.displayName ?? '…'}>
                  <Avatar className="size-5">
                    <AvatarFallback className="text-[0.625rem]">{initials}</AvatarFallback>
                  </Avatar>
                  <span className="min-w-0 flex-1 truncate">{user?.displayName ?? '…'}</span>
                  <span className="font-mono text-[0.62rem] uppercase tracking-wide text-muted-foreground">
                    Enseignant
                  </span>
                </SidebarMenuButton>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" side="top">
                <DropdownMenuLabel>{user?.email}</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to="/enseignant/parametres">Paramètres</Link>
                </DropdownMenuItem>
                <DropdownMenuItem onClick={handleLogout}>Déconnexion</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
