# Audit UI Indépendant — TsimokaAI Frontend

**Date** : 2026-09-27  
**Scope** : Frontend React + TypeScript (TanStack Router, TanStack Query, shadcn/ui, Tailwind v4)  
**Méthode** : Revue exhaustive contre les Web Interface Guidelines (Vercel) + contrat de design TsimokaAI  
**Fichiers analysés** : 40+ composants/pages (UI primitives, layouts, features, routes)

---

## Résumé exécutif

L'interface est **solide, cohérente et bien architecturée**. Le système de design (tokens OKLCH, 2 surfaces Papier/Ardoise, 3 polices) est respecté partout. Les composants shadcn/ui sont correctement wrappés. Les patterns TanStack Router (loaders, beforeLoad guards) et TanStack Query sont bien utilisés.

**Problèmes majeurs** : accessibilité formulaires (labels manquants, autocomplete), focus visible incomplet sur certains composants custom, HTML sémantique perfectible (nav, buttons), états empty/loading/error inégaux, gestion reduced-motion perfectible.

---

## Ce qui fonctionne déjà et qu'il faut préserver

| Élément | Localisation | Pourquoi c'est bien |
|---------|--------------|---------------------|
| **Système de tokens OKLCH centralisé** | `src/styles/globals.css:18-87` | Source unique de vérité, mapping shadcn propre, surfaces Papier/Ardoise isolées |
| **3 rôles typo stricts** | `globals.css:127-131, 199-201` | Fraunces (titres), IBM Plex Sans (UI), IBM Plex Mono (data) — jamais mélangés |
| **Sidebar collapsible persistée** | `src/components/ui/sidebar.tsx` + `AppSidebar.tsx` | Cookie + raccourci clavier (Ctrl+B) + tooltip en mode collapsed + rail accessible |
| **Layouts pathless bien séparés** | `routes/_app/route.tsx`, `routes/_public/route.tsx`, `routes/enseignant/route.tsx` | Guards centralisés, pas de duplication, redirection rôle propre |
| **Chat Ardoise isolé** | `ChatPage.tsx:34, 53-55, 84-87` | `surface-ardoise` scope strict, popovers restent Papier (citation chips) |
| **Fiche verticale "spine card"** | `SpineCard.tsx:24-48` | Writing-mode vertical-rl, gradient overlay, tag couleur disciplinaire — signature visuelle forte |
| **MessageRenderer structuré** | `MessageRenderer.tsx` | Blocs typés (CODE, MERMAID, MATH, IMAGE, MARKDOWN), tables, lists, blockquotes — pas de dangerouslySetInnerHTML |
| **CitationChips accordéon** | `CitationChips.tsx` | Traçabilité RAG explicite, fallback chunkIds, pas de tooltip-only |
| **Chalk reveal respects prefers-reduced-motion** | `use-chalk-reveal.ts:18-22` | Détection native, fallback instantané |
| **Forms avec Label htmlFor** | `LoginForm.tsx:22-23`, `RegisterForm.tsx:30-31`, `CreateEspaceModal.tsx:58-59` | Association label/input correcte sur les formulaires critiques |
| **Focus visible global** | `globals.css:220-223` | `outline: 2px dashed var(--attention)` sur `:focus-visible` partout |
| **Loading states text ends with …** | `DocumentsPage.tsx:99`, `ChatInput.tsx:27`, `CreateEspaceModal.tsx:77` | Convention "Loading…" respectée |
| **Numbers tabular via font-mono** | Partout (badges, stats, dates) | `font-mono` + `text-[0.65rem]` pour alignement colonne |
| **Destructive actions = confirm modal** | `DocumentsPage.tsx:157-160`, `MembresPage.tsx:49-55` | `window.confirm` avant suppression |
| **TanStack Router loaders** | `routes/_app/espaces/$spaceId/fiches/$ficheId.tsx:14-19` | Préchargement data avant render, pas de spinners en cascade |
| **Error boundaries implicites** | `PersonaModal.tsx:105-110`, `EtagereGrid.tsx:23-29` | isLoading/isError gérés au niveau composant |

---

## Problèmes identifiés

### P0 — Critique (accessibilité, régression fonctionnelle)

#### 1. `src/features/chat/components/ChatInput.tsx:24-30`
**Problème** : `<input>` sans `<label>` ni `aria-label` — placeholder seulement  
**Impact** : Lecteurs d'écran n'annoncent pas la fonction du champ ; échecs WCAG 1.3.1, 3.3.2  
**Correction** : Ajouter `<label htmlFor="chat-input" className="sr-only">Message</label>` + `id="chat-input"` sur l'input

#### 2. `src/components/ui/input.tsx:7-18`
**Problème** : Composant `Input` n'impose pas `autocomplete` ni `name` — chaque usage doit le faire manuellement  
**Impact** : Gestionnaire de mots de passe déclenché sur champs non-auth, pas d'autofill utile  
**Correction** : Props par défaut `autocomplete="off"` (sauf type email/password) + warning TypeScript si `name` manquant

#### 3. `src/routes/_public/accueil.tsx:63-68`
**Problème** : `<Button asChild><Link>…` — navigation via button au lieu de `<a>` natif  
**Impact** : Cmd/Ctrl+click, middle-click, "Open in new tab" cassés ; pas de préchargement navigateur  
**Correction** : Utiliser `<Link>` directement avec classes Tailwind, ou `asChild` sur `<a>` pas `<button>`

#### 4. `src/components/shared/AppSidebar.tsx:96-99` + `AppSidebarEnseignant.tsx:162-166`
**Problème** : `<Bell>` icon-only button sans `aria-label` dans DropdownMenuTrigger  
**Impact** : Lecteur d'écran : "button" sans contexte  
**Correction** : `aria-label="Rappels"` sur le bouton wrapper

#### 5. `src/components/ui/sidebar.tsx:282-304` (SidebarRail)
**Problème** : `<button>` avec `tabIndex={-1}` — exclut du tab order clavier  
**Impact** : Impossible d'ouvrir/fermer la sidebar au clavier sans raccourci Ctrl+B  
**Correction** : Retirer `tabIndex={-1}`, ajouter `onKeyDown` pour Enter/Space

#### 6. `src/features/espaces/components/JoinEspaceModal.tsx:68-77`
**Problème** : `onChange={(e) => setCode(e.target.value.toUpperCase())}` — mutation pendant saisie casse IME/composition  
**Impact** : Saisie japonaise/chinoise/coréenne cassée, curseur saute  
**Correction** : `onBlur` pour uppercase, ou `text-transform: uppercase` CSS + validation submit

---

### P1 — Majeur (cohérence, UX dégradée, dette technique)

#### 7. `src/components/ui/button.tsx:8` — `outline-none` sans focus-visible replacement complet
**Problème** : `outline-none` + `focus-visible:ring-2` OK globalement MAIS `globals.css:220` force `outline: 2px dashed` — double focus ring possible  
**Impact** : Incohérence visuelle focus (ring + dashed outline)  
**Correction** : Retirer `outline-none` du Button, laisser `globals.css` gérer :focus-visible globalement

#### 8. `src/components/ui/accordion.tsx:36`
**Problème** : `transition-all` — anti-pattern performance  
**Impact** : Layout thrash sur chaque propriété animable  
**Correction** : `transition-[height,opacity,transform]` explicite

#### 9. `src/routes/_app/tableau-de-bord.tsx:32-42` + `enseignant/tableau-de-bord.tsx:60-71`
**Problème** : `<select>` natif sans `<label>` associé (aria-label seulement) + pas de `id`  
**Impact** : Click sur label ne focus pas le select ; accessibilité réduite  
**Correction** : Wrapper dans `<Label htmlFor="space-select">` + `id="space-select"`

#### 10. `src/features/espaces/components/EtagereFiltres.tsx:44-69`
**Problème** : `<select>` natifs stylisés sans `Label` — même problème #9  
**Impact** : Même impact accessibilité  
**Correction** : `<Label htmlFor>` + `id` sur chaque select

#### 11. `src/components/shared/SpaceLayout.tsx:27-29`
**Problème** : `<a href={backTo}>` pour navigation interne — recharge page complète  
**Impact** : Perf dégradée, perd état client (sidebar, scroll), casse SPA  
**Correction** : `<Link to={backTo}>` TanStack Router

#### 12. `src/components/shared/ChatPage.tsx:116-128`
**Problème** : `<select>` mobile pour conversations — même problème #9 + pas de `Label`  
**Impact** : Accessibilité mobile dégradée  
**Correction** : `<Label htmlFor="conversation-select" className="sr-only">` + `id`

#### 13. `src/features/chat/components/ChatMessage.tsx:26-34` (PersonaInfoButton)
**Problème** : `<button>` avec `title` + `aria-label` MAIS `Info` icon sans `aria-hidden="true"`  
**Impact** : Lecteur d'écran annonce "Info" + "Voir le persona actif" (redondant)  
**Correction** : `<Info className="size-3.5" aria-hidden="true" />`

#### 14. `src/components/ui/sheet.tsx:78` + `dialog.tsx:51`
**Problème** : Close button avec `XIcon` sans `aria-hidden="true"` + `sr-only` seulement sur span  
**Impact** : Icon annoncé "X" puis "Close"  
**Correction** : `aria-hidden="true"` sur l'icône

#### 15. `src/features/fiches/components/FicheCard.tsx:53` + `FicheCard.tsx:66`
**Problème** : `AccordionTrigger` avec `className="hover:no-underline"` — underline natif retiré au hover  
**Impact** : Perte d'affordance clic (pas de feedback visuel standard)  
**Correction** : Garder underline ou remplacer par `bg-muted/50` au hover

#### 16. `src/routes/_public/accueil.tsx:45`
**Problème** : `&eacute;` `&eacute;` dans JSX — entités HTML dans React (devrait être `é`)  
**Impact** : Affiché littéralement si pas render HTML, incohérence  
**Correction** : `é` directement (fichier UTF-8)

#### 17. `src/components/ui/avatar.tsx:25`
**Problème** : `bg-tag-droit-shs` hardcodé pour fallback — devrait utiliser token neutre ou hash du name  
**Impact** : Avatars tous même couleur, pas de variété visuelle  
**Correction** : Hash du `displayName` pour pick parmi tags disciplinaires

#### 18. `src/components/shared/AppSidebar.tsx:170-172` + `AppSidebarEnseignant.tsx:164-166`
**Problème** : Badge notification `bg-attention` sans `aria-label`/`aria-live`  
**Impact** : Nouveau rappel non annoncé aux lecteurs d'écran  
**Correction** : `aria-live="polite"` sur conteneur + `aria-label="X rappels non lus"`

#### 19. `src/features/espaces/components/CreateEspaceModal.tsx:63-68`
**Problème** : `subjectTag` Input sans `autocomplete="off"` — déclenche password manager  
**Impact** : UX parasite (suggestion email/mdp)  
**Correction** : `autocomplete="off"` sur champs non-auth

#### 20. `src/features/espaces/components/DocumentsPage.tsx:86-90`
**Problème** : Zone drag-and-drop `onClick={() => fileInputRef.current?.click()}` sur `<div>` — pas keyboard accessible  
**Impact** : Impossible d'ouvrir sélecteur fichier au clavier (Enter/Space)  
**Correction** : Wrapper dans `<button type="button">` ou rendre `<input>` focusable visuellement (opacity 0 + position absolute)

---

### P2 — Mineur (polish, cohérence visuelle, DX)

#### 21. `src/components/ui/input.tsx:11-15`
**Problème** : `placeholder:text-muted-foreground` mais pas `placeholder:font-mono` pour champs codes/IDs (ex: JoinEspaceModal)  
**Impact** : Incohérence typo mono vs sans sur placeholders techniques  
**Correction** : Variant `Input` avec `as="input"` + `mono` prop, ou classe utilitaire `placeholder:font-mono`

#### 22. `src/components/shared/TeacherDashboardBlocks.tsx:116-128`
**Problème** : `<select>` étudiant sans `Label` + options tronquées (userId.slice) — info perte contexte  
**Impact** : Difficile d'identifier l'étudiant sans connaître les IDs  
**Correction** : Afficher `displayName` si dispo, sinon fallback ; ajouter `<Label>`

#### 23. `src/routes/_app/espaces/$spaceId/fiches/index.tsx:43`
**Problème** : `getTagColorClass(space?.subjectTag)` sur `<span className="w-1.5">` — barre verticale colorée mais pas accessible (couleur seule)  
**Impact** : Info tag disciplinaire perdue si daltonien / mode contraste élevé  
**Correction** : Ajouter `title={space?.subjectTag}` ou tooltip

#### 24. `src/features/espaces/components/SpineCard.tsx:34-41`
**Problème** : Date + documentCount en `font-mono text-[0.58rem] opacity-85` — taille trop petite (9.3px)  
**Impact** : Lisibilité limite, surtout sur mobile  
**Correction** : `text-[0.65rem]` (10.4px) minimum per guidelines

#### 25. `src/components/shared/AppSidebar.tsx:71` + `AppSidebarEnseignant.tsx:122`
**Problème** : `SidebarGroupLabel` en `text-[0.65rem]` (10.4px) uppercase tracking-wide — OK mais `text-muted-foreground` sur surface Ardoise = `craie-muted` (contraste ?)  
**Impact** : Vérifier ratio contraste 4.5:1 sur `craie-muted` / `ardoise-bg`  
**Correction** : Audit contraste ou `text-craie/70` explicite

#### 26. `src/routes/onboarding/bienvenue.tsx:38-51`
**Problème** : Boutons rôle `Button` sans `aria-pressed` pour indiquer sélection (état visuel seulement)  
**Impact** : État non communiqué aux tech assistives  
**Correction** : `aria-pressed={roleChosen && role === 'STUDENT'}` etc.

#### 27. `src/features/chat/components/MessageRenderer.tsx:44-47`
**Problème** : `<img>` sans `width`/`height` explicites — CLS possible  
**Impact** : Layout shift au chargement image  
**Correction** : Props `width`/`height` requis sur bloc IMAGE backend, ou aspect-ratio CSS

#### 28. `src/features/chat/components/CitationChips.tsx:41-44`
**Problème** : Fallback chip `title={id}` — UUID brut en tooltip natif (pas accessible mobile, pas stylable)  
**Impact** : Info technique uniquement au hover desktop  
**Correction** : Tooltip shadcn/ui ou texte visible `[Source ${i+1}]`

#### 29. `src/routes/_app/espaces/$spaceId/chat.tsx` (redirect racine)
**Problème** : `/espaces/$spaceId` redirecte vers `/chat` via `beforeLoad` — URL ne reflète pas l'état (onglet actif)  
**Impact** : Pas de deep-link direct vers Documents/Membres/Quiz ; refresh perd l'onglet  
**Correction** : Synchroniser onglet actif dans URL (ex: `/espaces/$spaceId?tab=chat`) via `nuqs` ou search params

#### 30. `src/components/ui/sidebar.tsx:476-496` (sidebarMenuButtonVariants)
**Problème** : `transition-[width,height,padding]` — `height`/`width` trigger layout recalc  
**Impact** : Animation moins fluide (non compositor-only)  
**Correction** : `transform: scaleX()` ou `width` seulement si contenu fixe

#### 31. `src/components/ui/button.tsx:12-17` (variants)
**Problème** : `shadow-xs` custom — non standard Tailwind, dépend `tw-animate-css`  
**Impact** : Dépendance implicite, pas documenté  
**Correction** : Documenter ou remplacer par `shadow-sm` standard

#### 32. `src/routes/_public/route.tsx:28-39` (PublicLayout header)
**Problème** : `<nav>` avec `<Link>` mais pas de `aria-label` — landmark non nommé  
**Impact** : Plusieurs `<nav>` dans page = confusion lecteurs d'écran  
**Correction** : `aria-label="Navigation principale"` sur header nav

#### 33. `src/components/shared/DocumentsPage.tsx:101-108`
**Problème** : `<input type="file" className="hidden">` — masqué visuellement mais pas `sr-only`, inaccessible clavier  
**Impact** : Même problème #20  
**Correction** : Pattern accessible : `position: absolute; opacity: 0; pointer-events: none` sur label visible

#### 34. `src/features/espaces/components/PersonaModal.tsx:165-172`
**Problème** : `<textarea>` sans `Label` associé (Label htmlFor présent mais `id` manquant sur textarea)  
**Impact** : Label non cliquable pour focus textarea  
**Correction** : `id="persona-edit"` sur textarea (déjà présent ligne 166) — **vérifier** : l'ID est bien là, OK

#### 35. `src/routes/_app/espaces/$spaceId/fiches/$ficheId.tsx:48`
**Problème** : `FicheValidationBadge` importé mais usage non visible dans extrait — vérifier si `aria-label` sur badge  
**Impact** : Badge status sans texte accessible  
**Correction** : Audit composant `FicheValidationBadge` (non lu)

---

### P3 — Cosmétique / Dette faible

#### 36. `src/styles/globals.css:220-223`
**Problème** : `:focus-visible { outline: 2px dashed var(--attention); outline-offset: 2px; }` — `dashed` peu commun, peut gêner certains utilisateurs  
**Impact** : Préférence subjective, pas de fail WCAG  
**Correction** : `solid` ou laisser choix utilisateur via CSS custom property

#### 37. `src/components/ui/tabs.tsx:28`
**Problème** : `data-[state=active]:border-tag-sciences` — couleur tag sciences hardcodée pour active state  
**Impact** : Incohérence si autre tag dominant dans l'espace  
**Correction** : Utiliser `--ring` ou `--primary` token sémantique

#### 38. `src/components/ui/badge.tsx` (non lu) — vérifier variants `succes`/`attention`/`erreur` mapping tokens
**Problème** : Variants custom non standard shadcn — mapping vers tokens OKLCH à vérifier  
**Impact** : Cohérence couleur si tokens changent  
**Correction** : S'assurer que `badge.tsx` utilise `bg-succes`, `bg-attention`, `bg-erreur` tokens

#### 39. `src/routes/_app/espaces/$spaceId/parametres.tsx` (non lu) — vérifier formulaires settings
**Problème** : Probablement même problèmes formulaires (#2, #9)  
**Impact** : Cohérence accessibilité  
**Correction** : Audit similaire

#### 40. `src/features/quiz/components/QuizCard.tsx` (non lu) — vérifier accessibilité carte quiz
**Problème** : Carte cliquable probable sans role/button sémantique  
**Impact** : Navigation clavier/lecteur d'écran  
**Correction** : Audit composant

---

## Problèmes les plus importants à traiter (Top 5)

| # | Fichier | Problème | Pourquoi priorité |
|---|---------|----------|-------------------|
| 1 | `ChatInput.tsx:24-30` | Input sans label/aria-label | Chat = cœur de l'app, utilisé en continu, blocage accessibilité majeur |
| 2 | `SidebarRail.tsx:282-304` | `tabIndex={-1}` exclut du clavier | Navigation principale inutilisable au clavier sans raccourci |
| 3 | `JoinEspaceModal.tsx:68-77` | `onChange` uppercase casse IME | Saisie internationale cassée (Japon, Chine, Corée, etc.) |
| 4 | `SpaceLayout.tsx:27-29` | `<a href>` navigation interne | Casse SPA, perf, état perdu — régression UX silencieuse |
| 5 | `Input.tsx:7-18` | Pas d'autocomplete/name par défaut | Propage à tous formulaires — dette systémique |

---

## Recommandations transverses

1. **Audit automatisé** : Ajouter `eslint-plugin-jsx-a11y` + `axe-core` en CI pour catch régressions
2. **Storybook a11y addon** : Tester composants UI isolément (Button, Input, Sidebar, Dialog, Accordion)
3. **Design tokens → Figma sync** : Exporter tokens OKLCH vers Figma pour design/dev parity
4. **Reduced-motion global** : Vérifier toutes animations (sidebar, accordion, sheet, dialog, chalk-reveal) respectent `prefers-reduced-motion`
5. **Focus management modals** : `DialogContent`/`SheetContent` — focus trap Radix OK, mais vérifier `initialFocus` sur premier input
6. **Error toasts aria-live** : Pas de toaster global vu — ajouter `Toaster` avec `aria-live="polite"` pour erreurs async
7. **Skip link** : Ajouter `<a href="#main" className="sr-only focus:not-sr-only">Aller au contenu</a>` dans `__root.tsx`
8. **HTML lang** : Vérifier `index.html` a `<html lang="fr">` + `dir="ltr"`

---

## Fichiers non audités (à revoir)

- `src/features/fiches/components/FicheValidationBadge.tsx`
- `src/features/fiches/components/AnnotationsSection.tsx`
- `src/features/fiches/components/ValidationSection.tsx`
- `src/features/fiches/components/ShareFicheModal.tsx`
- `src/features/fiches/components/GenerateFicheModal.tsx`
- `src/features/quiz/components/QuizCard.tsx`
- `src/features/quiz/components/GenerateQuizModal.tsx`
- `src/routes/_app/espaces/$spaceId/quiz/$quizId/take.tsx`
- `src/routes/_app/espaces/$spaceId/parametres.tsx`
- `src/routes/enseignant/espaces/$spaceId/fiches-a-valider.tsx`
- `src/routes/onboarding/creer-espace.tsx`
- `src/routes/_public/connexion.tsx`, `inscription.tsx`, `mot-de-passe-oublie.tsx`

---

*Audit réalisé en lecture seule — aucune modification du code source.*
