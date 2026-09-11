import { Link, useLocation, useNavigate } from '@tanstack/react-router';
import { GraduationCap, Home, LayoutGrid, ClipboardCheck, Settings, Bell } from 'lucide-react';

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
const NAV_PRINCIPAL = [
  { to: '/enseignant', label: 'Mes espaces', icon: Home },
  { to: '/enseignant/tableau-de-bord', label: 'Tableau de bord', icon: LayoutGrid },
  { to: '/enseignant/fiches-a-valider', label: 'Fiches à valider', icon: ClipboardCheck },
] as const;

const NAV_SECONDAIRE = [
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
        <Link to={to} activeOptions={{ exact: true }} className="text-craie">
          <Icon />
          <span>{label}</span>
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}

export function AppSidebarEnseignant() {
  const { data: user } = useSession();
  const { data: rappels } = useRappels();
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
              <Link to="/enseignant" className="font-display text-base font-semibold text-craie">
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
          <SidebarGroupLabel className="font-mono text-[0.65rem] uppercase tracking-[0.14em] text-muted-foreground">
            Principal
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {NAV_PRINCIPAL.map((item) => (
                <NavItem key={item.to} {...item} />
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
                  <span className="min-w-0 flex-1 truncate text-craie">{user?.displayName ?? '…'}</span>
                  <span className="ml-auto flex items-center gap-2">
                    <span className="font-mono text-[0.62rem] uppercase tracking-wide text-muted-foreground">
                      Enseignant
                    </span>
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
