# Audit UI — Frontend TsimokaAI (React + TypeScript)

**Date :** 2026-09-27
**Périmètre :** `frontend/src/` existant — amélioration, pas reconstruction. Logique métier / contrats API hors périmètre.
**Référentiel :** skill `web-design-guidelines` chargé explicitement + guidelines fraîches `vercel-labs/web-interface-guidelines` (a11y, focus, forms, animation, typo, images, perf, navigation/state, touch, layout, theming, i18n, hydration, hover, anti-patterns) + contrat de design existant (`styles/globals.css` : surfaces Papier/Ardoise, tokens OKLCH, Fraunces / Plex Sans / Mono, rayons 4px vs 2px fiche).
**Méthode :** lecture code statique (pas de runtime navigateur). Aucun fichier source modifié. Un élément n'est signalé que s'il est observable dans le code avec impact utilisateur démontrable, pas sur préférence esthétique.
**Priorités :** P0 bloquant / perte / danger · P1 dégrade fortement UX-a11y-confiance · P2 friction modérée / incohérence · P3 polish.

---

## 0. Cartographie de l'existant (base de l'audit)

### 0.1 Structure des pages (TanStack Router, `src/routes/`)
- `__root.tsx:28-42` : enveloppe neutre (Outlet + devtools), pas de surface imposée — conforme au contrat.
- `_public/route.tsx:27-47` : layout Papier (`bg-papier-bg`), `header/nav/main/footer`, sans `h1`, `nav` sans `aria-label`, `main` sans `id`.
- `_public/` : `accueil.tsx` (seul `h1` public + `h3` sans `h2`, section en `p`), `connexion.tsx` (`h2` orphelin + `validateSearch` non consommé), `inscription.tsx` (`h2` orphelin), `mot-de-passe-oublie.tsx` (2 branches `h2`, `autoFocus`, `envoye=true` même en échec, `isError` jamais rendu).
- `onboarding/route.tsx:9-11` : centré Papier, sans landmark. `bienvenue.tsx` : choix rôle STUDENT/ENSEIGNANT (2 `h1` conditionnels, erreurs `updateProfile` jamais affichées), `creer-espace.tsx` : `h1` dans `form`, `Input` monoligne pour description, erreurs jamais rendues.
- `_app/route.tsx:50-55` / `enseignant/route.tsx:32-40` : `SidebarProvider > AppSidebar + SidebarInset > Outlet`, `h-svh`, pas de skip-link.
- `_app/index.tsx:43-106` : `h1 L'étagère` + `EtagereGrid` + bloc `Reprendre où j'en étais` (`Link` chat/fiche). `tableau-de-bord.tsx:24-156` : `h1 Ma progression` + 5× `h3` (saut `h2`), `dashboard &&` sans loading/erreur. `objectifs.tsx:77-269` : `h1 Objectifs` + `h2 Badges/Rappels/Suivi`, mélange objectifs mono-espace + badges/rappels transverses (commentaire assumé `18-26`). `mes-fiches.tsx`, `parametres.tsx:68-155` (`h1` + `CardTitle h3` + `h3 Mot de passe` dans `form`).
- `espaces/$spaceId/` : `route.tsx:17-30` (TABS Chat/Fiches/Documents/Quiz/Membres + Paramètres owner, `backTo="/"`), `index.tsx:9-11` (redirect → chat), `chat.tsx:14` (`ChatPage showSpaceBar`), `documents.tsx`, `fiches/index.tsx` (`h2 Fiches`), `fiches/$ficheId.tsx` (montage `FicheCard h3` avant `h2 Annotations/Validation`), `quiz/index.tsx` (`h2 Quiz`), `quiz/$quizId/index.tsx` (score + `Badge %`), `quiz/$quizId/take.tsx:19-126` (`return null` si pas de quiz, `useState` step/answers/submitted, `Progress`).
- `enseignant/` : miroir (`index.tsx`, `tableau-de-bord.tsx:24 h1` + blocs, `fiches-a-valider.tsx:59-282` seul bon pattern `Dialog` rejet, `espaces/$spaceId/` + `dashboard.tsx`, `chat.tsx:14 mode="enseignant"`, `documents.tsx` identique étudiant).
- `main.tsx:17-23` : `defaultPreload:intent`, `scrollRestoration:true`, `defaultNotFoundComponent <p>` sans heading.

### 0.2 Composants réutilisables
- `components/ui/*` (shadcn new-york, `components.json:2-10`) : `button.tsx:8` (ring, `outline-none` + remplacement, pas de spinner interne), `input.tsx:7-19` (`min-w-0`, `aria-invalid:` prêt mais jamais alimenté), `label.tsx:8-16` (style mono uppercase imposé), `dialog.tsx:32-86` (Radix, `Close sr-only Fermer`, sans `max-h/overflow/overscroll`), `tabs.tsx:30/39` (trigger ring / content `outline-none`), `dropdown-menu.tsx:38` (`outline-none` seul), `accordion.tsx:36` (`transition-all` + ring), `sidebar.tsx` (rail `tabIndex -1` + `Toggle Sidebar` anglais, `SheetTitle Sidebar` anglais, mobile `Sheet [&>button]:hidden` sans croix, `h-8` <44px, `after:-inset-2` compensation partielle), `skeleton.tsx` (existe mais quasi jamais utilisé), `progress.tsx:6-17` (`value` non retransmise), `avatar.tsx`, `badge.tsx:8` (base saine `uppercase/shrink-0`), `card.tsx:19-27` (`CardTitle h3` figé), `tooltip.tsx:43` (seul `text-balance` du repo).
- `components/shared/*` : `AppSidebar.tsx:51-199` / `AppSidebarEnseignant.tsx` (variantes séparées assumées, `surface-ardoise` permanente, `NavItem` `isActive` via `pathname`, `UserMenu` rappels `slice(0,5)` + `DropdownMenuItem disabled`), `SpaceLayout.tsx:17-71` (`<a href>`, `h1 {space?.name}` vide en chargement, `nav` sans label ni scroll, `TabLink` sans `aria-current`), `ChatPage.tsx:34-238` (états `!conversations`, `activeId ?? conversations[0]`, `SidebarInset surface-ardoise` + bandeau Papier + rail Papier, `select` mobile sans label, bouton collapsed icône seule), `DocumentsPage.tsx:34-174` (dropzone `div onClick`, `input hidden`, `window.confirm`, `Badge` statuts), `MembresPage.tsx` / `MembresPageEnseignant.tsx` (doublons, `window.confirm` + `window.location.href`, `Retirer` sans confirmation, placeholders sans labels), `TeacherDashboardBlocks.tsx` (`select aria-label="Étudiant"` générique + `userId.slice(0,8)`, `slice(0,10)` sans voir-plus).
- `features/*` : `auth/LoginForm.tsx, RegisterForm.tsx` (labels ok, `type` ok, zéro `name/autocomplete`, erreur globale brute), `chat/` (`ChatThread.tsx` : `scrollIntoView smooth` sans reduced-motion, `isLoading` texte seul, vide non géré ; `ChatInput.tsx` : input sans label, `focus:outline-none` ; `ChatMessage.tsx` : `aria-label` + `sr-only` redondants, icône non masquée ; `MessageRenderer.tsx` : `#→h2` assumé, `img` sans dimensions, `table` sans `scope`, conteneurs scroll non focusables, liens `target _blank` sans signal ; `CodeBlock.tsx` : copy sans live ni try/catch ; `MermaidBlock.tsx/MathBlock.tsx` : sans `role=img`/texte alt, init sans reduced-motion ; `CitationChips.tsx` : `Accordion`, fallback `title` souris seule ; `use-chalk-reveal.ts:18` : seul `prefers-reduced-motion` du repo), `quiz/` (`QuizQuestionCard.tsx` : `label` enveloppant ok, pastille `size-4` 16px ; `GenerateQuizModal.tsx` : radios sans `fieldset/legend`, `DocRadio button>input` invalide ; `ShareQuizModal.tsx` : `role=radiogroup` sans label, cible ~32px ; `QuizResultsPanel.tsx` : score en `p` sans heading), `fiches/` (`FicheCard.tsx` : `h3` + `h4`, `Accordion`, erreur `JSON invalide` brute ; `GenerateFicheModal.tsx` : même pattern + `DocCheckbox button>Checkbox` ; `ShareFicheModal.tsx` : idem partage ; `ValidationSection.tsx` : `textarea` sans label + `focus-visible:outline-none` sans ring ; `AnnotationsSection.tsx` : 2 `Input` placeholder-only), `espaces/` (`SpineCard.tsx` : `hover:-translate-y-1` sans focus, `useConversations` par carte = N+1 ; `EtagereGrid.tsx` : `isLoading/isError` gérés mais erreur sans retry ; `EtagereFiltres.tsx` : `aria-label` ok, `h-9/h-8` <44px ; `EtagereSection.tsx` : `section aria-label`, `overflow-x-auto` ; `CreateEspaceModal.tsx` / `JoinEspaceModal.tsx` : `DialogTitle/Description` ok, seul `placeholder …` conforme est `CreateEspaceModal:65`, `JoinModal` sans `inputMode/pattern/autoCapitalize`, erreur seule inline typée mais sans live/focus ; `PersonaModal.tsx` : loading/erreur/empty gérés en texte seul, `Label` sans `htmlFor` en lecture, `textarea` ok, icône `RefreshCw` sans `aria-hidden/spin`, `DialogContent sm:max-w-lg` seul responsive modal).

### 0.3 Système de styles
- `styles/globals.css:1-224` : Tailwind v4 + `tw-animate-css` + KaTeX. Tokens contrat en OKLCH (`papier`, `ardoise`, tags disciplinaires réservés, `succes/attention/erreur`, `--radius 4px` vs `--radius-fiche 2px`). Mapping sémantique shadcn (`--background` etc.). `.surface-ardoise:98-124` redéfinit tokens (contrat : uniquement wrapper route chat, jamais mélangé). Typo 3 rôles (Fraunces display / Plex Sans UI / Plex Mono utilitaire). Base `:focus-visible:220-223` (`2px dashed var(--attention)`) correcte, `*:213-214` impose `border-border outline-ring/50`. Utilitaires bruts `bg-papier-*` vs sémantiques `bg-background` documentés.
- Écarts constatés : `AppSidebar` en `surface-ardoise` permanente alors que contenu `_app` est Papier (mélange proscrit) ; `ChatPage` mélange Ardoise + îlots Papier + `bg-muted/secondary` ; `index.html:5` viewport sans `viewport-fit=cover`, sans `theme-color`, sans `color-scheme` (grep 0), fonts Google CDN avec `preconnect/display=swap` ok mais `preload as=font` absent et self-host `@fontsource` seulement en commentaire.

### 0.4 Navigation
- Routeur précharge (`intent`), `Link` + `activeOptions` bien utilisés (sidebar, étagère, quiz). Fautes : `SpaceLayout.tsx:27 <a href={backTo}>` (reload), `MembresPage.tsx:52 / MembresPageEnseignant.tsx:58 window.location.href`, dropzone `div onClick`.
- État non deep-linké : `ChatPage:39 activeConversationId`, `EtagereGrid:21 filtres`, `EtagereFiltres`, `tableau-de-bord:17 spaceId`, `objectifs:43 spaceId`, `fiches-a-valider:207 search/selectedSpace`, `quiz take:29-31 step/answers/submitted` — tous `useState` mémoire, perdus au reload, non partageables. Seul `connexion.tsx:15 validateSearch` existe et n'est pas consommé.
- Pas de skip-link (`__root`, `_app`, `enseignant`, `_public`, `SpaceLayout`), `main` sans `id`, `nav` sans `aria-label`, `SidebarRail` exclu clavier (`tabIndex -1`), drawer mobile sans croix visible (`sidebar.tsx:190`).
- Destructif : bon pattern `Dialog` avec commentaire obligatoire pour rejet fiche (`fiches-a-valider:168-198`) et `confirmDelete` inline pour compte/espace ; mauvais : `window.confirm` natif (quitter espace, supprimer groupe/document) et surtout **zéro confirmation** pour `Retirer membre` et `Supprimer rappel`.

### 0.5 Parcours utilisateur principaux
- Découverte → inscription/connexion → onboarding rôle → créer/rejoindre espace → étagère → chat (défaut via `index redirect`) → documents → fiches → quiz → tableaux de bord → objectifs/badges/rappels → paramètres. Parcours enseignant parallèle (dashboard, validation, persona).
- Points de rupture : `take.tsx:64 return null` (quiz vide = blanc), `tableau-de-bord:46 dashboard &&` (vide partiel), `ChatPage:51 confond chargement/vide`, `ChatPage:80 return null` (conversations présentes mais `activeId` nul = blanc), `ChatThread` vide = input seul, `mot-de-passe-oublie` succès affiché même en échec.

### 0.6 États loading / empty / error
- Bonnes bases texte : `EtagereGrid:23-37` (chargement/erreur/vide), `PersonaModal:106-158` (chargement/erreur/vide lecture), `DocumentsPage:98/111` (envoi/vide), `objectifs:125-214` (vides), `TeacherDashboardBlocks:56-145` (vides).
- Manques systémiques : **zéro `aria-live` / `role=status/alert` dans `src`** (grep 0), `Skeleton` quasi inutilisé, erreurs API brutes sans next step (`EtagereGrid:28`, `ShareQuiz/ShareFiche/GenerateFiche`, `FicheCard:79`), erreurs jamais rendues (`Login/Register/creer-espace/CreateEspace/mot-de-passe-oublie isError`, `onboarding updateProfile`), pas de retry (`EtagereGrid`, `PersonaModal`), `isLoading` ignorés (`ChatPage:35`, `DocumentsPage:36`, `tableau-de-bord`, `objectifs`).

### 0.7 Formulaires et interactions
- Labels associés quand `Label htmlFor + Input id` (auth, creer-espace, modales, `JoinModal:66`) ; échecs placeholder-only (`parametres:97-117` 3× password, `objectifs:131-144/228-245`, `AnnotationsSection:50-60`, `MembresPage:122-181`).
- `type` corrects (`email/password/date/file/search`) mais **zéro `autocomplete`/`name`** (grep 0 sauf `name=` radio partage), pas d'`inputmode` code, pas de `spellCheck=false`, pas d'`aria-invalid/describedby` (alors que `input.tsx:15` le prévoit).
- Submit désactivé + libellé `…` partout (conforme : pas de double-submit), mais **zéro spinner** (`Loader2/animate-spin/aria-busy` 0 dans périmètre formulaires), zéro focus première erreur, zéro `fieldset/legend` radios.
- Placeholders majoritairement sans `…` (seuls `EtagereFiltres:37`, `CreateEspaceModal:65`, `ChatInput:27`, `PersonaModal:170`, `AnnotationsSection:51`, `fiches-a-valider:180` conformes).
- `autoFocus` sur `mot-de-passe-oublie:62` et `JoinEspaceModal:71` (mobile non exclu). Pas de `beforeunload` / `useBlocker` (perte saisie silencieuse).

### 0.8 Conventions visuelles déjà présentes (à préserver)
- Papier par défaut, Ardoise chat, tags disciplinaires réservés, rayons 4px vs 2px fiche, Fraunces titres / Sans UI / Mono eyebrows-timestamps-badges, `truncate + min-w-0`, `hover:` systématique, badges `succes/attention/erreur`, `rounded-fiche`, `border-dashed` empty, `font-mono` chiffres (mais sans `tabular-nums`), `leading-relaxed`, `text-encre-muted`.

---

## 1. Conformité / accessibilité (règles skill)

### C-01 — `features/chat/components/ChatInput.tsx:24-29` — Champ chat sans label + focus supprimé sans remplacement — **P0**
- **Problème :** `input` sans `<label>`, sans `id`, sans `aria-label`, et `focus:outline-none` sans `focus-visible:` de remplacement.
- **Pourquoi :** viole 2 règles bloquantes (contrôle de formulaire étiqueté ; jamais `outline-none` sans remplacement). La base globale `globals.css:220` est annulée ici.
- **Impact :** lecteurs d'écran annoncent « champ vide » sans nom sur le parcours cœur ; clavier ne voit plus le focus dans le chat.
- **Correction :** ajouter `id="chat-input" + <label className="sr-only" htmlFor>` ou `aria-label="Écris ta question sur le cours"`, remplacer `focus:outline-none` par `focus-visible:ring-2 ring-ring` (comme `ui/input`), garder le placeholder avec `…`.

### C-02 — `components/shared/DocumentsPage.tsx:81-108` — Dropzone `div onClick` non clavier — **P0**
- **Problème :** `div onDragOver/onDrop/onClick=>fileInput.click()` sans `role`, `tabIndex`, `onKeyDown`, `aria-label` ; `input hidden` non focusable ; seul `hover:` sans `focus-within:`.
- **Pourquoi :** `<div>` cliquable au lieu de `<button>` ; clavier/souris non équivalents.
- **Impact :** dépôt impossible au clavier et au lecteur d'écran ; focus invisible.
- **Correction :** transformer en `<button type="button" aria-label="Déposer des fichiers">` ou garder `div` avec `role="button" tabIndex={0} onKeyDown Enter/Espace + focus-visible:ring-2 + focus-within:` ; exposer `input` via `sr-only` au lieu de `hidden` si besoin ; ajouter `aria-describedby` formats acceptés.

### C-03 — `features/fiches/components/ValidationSection.tsx:90-96` + `AnnotationsSection.tsx:50-60` + `routes/_app/parametres.tsx:97-117` + `routes/_app/objectifs.tsx:130-148/217-246` + `components/shared/MembresPage.tsx:122-181` — Champs placeholder-only — **P0**
- **Problème :** `textarea` commentaire, 2 inputs annotation, 3 inputs password, inputs objectif/rappel, inputs membre/groupe n'ont que `placeholder`, sans `Label`.
- **Pourquoi :** placeholder n'est pas un label (disparaît, non lu de façon fiable, contraste faible).
- **Impact :** AT ne nomme pas les champs ; mot de passe sans label = échec critique auth/compte.
- **Correction :** ajouter `Label htmlFor + Input id` visibles (ou `sr-only` si contrainte), lier erreurs via `aria-describedby + aria-invalid`.

### C-04 — Tout `src` (grep 0) — Aucun `aria-live` / `role=status/alert` — **P1**
- **Problème :** 0 occurrence `aria-live` : upload `DocumentsPage:98-99`, streaming chat, `Envoi/Creation…`, erreurs `LoginForm:36-41`, `JoinEspaceModal:78`, `PersonaModal:173-182`, `parametres:119-120`, copies `Copié`, `CodeBlock Copié !`.
- **Pourquoi :** mises à jour async/toasts/validations exigent `aria-live="polite"` (erreurs `role="alert"`).
- **Impact :** utilisateurs AT ne perçoivent ni succès ni erreurs.
- **Correction :** `role="status" aria-live="polite"` sur conteneurs chargement/succès, `role="alert"` erreurs inline, `aria-live` sur `CodeBlock` swap et copies.

### C-05 — `features/auth/components/LoginForm.tsx:23-33` + `RegisterForm.tsx:30-51` + `routes/_app/parametres.tsx:97-117` — Zéro `autocomplete` / `name` — **P1**
- **Problème :** `type=email/password` corrects mais sans `autoComplete="email/current-password/new-password/name"`, sans `name`, sans `spellCheck={false}`.
- **Pourquoi :** règle forms (`autocomplete + name`, `type/inputmode` corrects, `spellCheck=false` emails/codes).
- **Impact :** gestionnaires mots de passe / claviers mobiles / remplissage auto cassés ; fautes sur codes.
- **Correction :** `email→name=email autocomplete=email spellCheck=false`, `login password→current-password`, `register/nouveau→new-password`, `displayName→name autocomplete=name`, code `JoinEspaceModal` → `autocomplete="one-time-code" inputMode="text" autoCapitalize="characters" spellCheck={false}`.

### C-06 — `components/shared/ChatPage.tsx:191-197` — Bouton icône seule sans nom en sidebar repliée — **P1**
- **Problème :** `Nouvelle conversation` ne rend son texte que si `state==='expanded'` ; en `collapsed` reste `Button + Plus` sans `aria-label`, seul `tooltip`.
- **Pourquoi :** boutons icône seule exigent `aria-label`; tooltip ≠ nom accessible.
- **Impact :** AT annonce « bouton » sans intitulé.
- **Correction :** `aria-label="Nouvelle conversation"` permanent sur le `Button`.

### C-07 — `features/quiz/components/GenerateQuizModal.tsx:272-296` + `features/fiches/components/GenerateFicheModal.tsx:241-249` — Interactifs imbriqués `button>input` — **P1**
- **Problème :** `DocRadio/DocCheckbox` = `button onClick > input radio/checkbox onChange`, double gestion, `px-2 py-1` ~24px, pastille `size-4` 16px.
- **Pourquoi :** HTML invalide, double annonce AT, petite cible.
- **Impact :** clic/clavier imprévisible, échec tactile.
- **Correction :** un seul `label` enveloppant + `input` natif (pattern déjà bon de `QuizQuestionCard:33-52`), cible ≥44px (`min-h-11 px-3 py-2.5`), pas de `button` parent.

### C-08 — `components/shared/ChatPage.tsx:116-128` + `routes/_app/tableau-de-bord.tsx:32-43` + `routes/_app/objectifs.tsx:113-121` + `components/shared/TeacherDashboardBlocks.tsx:116-130` — Selects sans nom ou générique — **P1**
- **Problème :** select mobile conversations sans `aria-label/Label` ; selects espace sans label ; select statut `bg-transparent text-xs` ~18px sans label ; select étudiant `aria-label="Étudiant"` générique + `userId.slice(0,8)…` illisible.
- **Pourquoi :** contrôles sans nom + cibles <44px.
- **Impact :** AT + tactile échouent sur navigation espace/statut/conversation.
- **Correction :** `aria-label` explicites (« Conversation active », « Espace actif », « Statut de l'objectif »), `Label` visible ou `sr-only`, hauteur ≥44px mobile, afficher `displayName` au lieu d'UUID tronqué.

### C-09 — `components/ui/progress.tsx:6-17` — `value` non transmise = progressbar sans valeur — **P1**
- **Problème :** `value` déstructurée mais non retransmise à `Root`.
- **Pourquoi :** `progressbar` sans `aria-valuenow/min/max`.
- **Impact :** progression quiz illisible aux AT (`quiz take:92`).
- **Correction :** retransmettre `value/max` à `ProgressPrimitive.Root` (ou `aria-valuenow/min/max` manuels).

### C-10 — `components/shared/SpaceLayout.tsx:27` + `MembresPage.tsx:52` + `MembresPageEnseignant.tsx:58` — Navigation par `<a>` / `location.href` — **P1**
- **Problème :** retour `<a href={backTo}>` (reload pleine page), `window.location.href=basePath` après quitter.
- **Pourquoi :** liens internes doivent être `<Link>` (Cmd/Ctrl+clic, molette, prefetch).
- **Impact :** perte état, rechargement, rupture SPA.
- **Correction :** `Link to={backTo}` + `navigate({to})` TanStack.

### C-11 — `__root.tsx:28-42`, `_app/route.tsx:50-55`, `enseignant/route.tsx:32-40`, `_public/route.tsx:27-47`, `SpaceLayout.tsx:24-55` — Pas de skip-link, `main` sans `id`, `nav` sans label — **P1**
- **Problème :** aucun lien d'évitement, `main` sans `id="main"`, `nav` sans `aria-label`, `SidebarRail:286-291 tabIndex -1`.
- **Pourquoi :** headings + skip-link exigés ; rail exclu clavier ne laisse que `Ctrl+B`.
- **Impact :** navigation clavier laborieuse (sidebar rejouée à chaque page).
- **Correction :** skip-link « Aller au contenu » → `id="main"`, `aria-label` navs (« Navigation principale », « Onglets espace »), rendre `SidebarRail` focusable avec `aria-label` français ou le laisser décoratif `aria-hidden` avec alternative clavier documentée.

### C-12 — `features/chat/components/MessageRenderer.tsx:43-54` — Image sans dimensions, `alt` vide, `src=''` possible — **P1**
- **Problème :** `img src={url ?? ''} alt={alt ?? ''}` sans `width/height`, sans `loading="lazy"`, `alt=''` fait passer un contenu pour décoratif, `src=''` = image cassée.
- **Pourquoi :** images exigent dimensions (CLS) + `alt` réel ou `alt=""` seulement si décoratif + lazy sous fold.
- **Impact :** CLS + contenu ignoré par AT + requête invalide.
- **Correction :** ne rendre `figure` que si `url` truthy, exiger `alt` non vide côté back ou repli texte, ajouter `width/height` (ou `aspect-ratio`) + `loading="lazy" decoding="async"`, `figcaption` déjà ok.

### C-13 — Headings : `tableau-de-bord.tsx:29/49-130` (`h1→h3`), `accueil.tsx:53/81` (`h1→h3` + section en `p`), `connexion/inscription/mot-de-passe-oublie` (`h2` sans `h1`), `quiz/index + fiches/index` (`h2` sans `h1`), `FicheCard:30 h3` avant `Annotations:38 h2`, `QuizQuestionCard:25 h4` orphelin, `QuizResultsPanel:20 p` au lieu de heading, `parametres:71/75/95` (`h1→h3`) — **P2**
- **Problème :** sauts et orphelins avérés en lecture.
- **Pourquoi :** hiérarchie `h1-h6` continue exigée pour navigation AT.
- **Impact :** plan de page incohérent, sections manquées.
- **Correction :** `h1` par page (layout ou page), sections en `h2`, cartes en `h3`, questions en `h3` (pas `h4`), score/résultats en `h2`, `CardTitle` paramétrable (`h2/h3` via prop) ou contourné par `aria-labelledby`.

### C-14 — Focus : `ui/tabs.tsx:39 Content outline-none`, `ui/dropdown-menu.tsx:38 outline-none` seul, `ValidationSection.tsx:95 focus-visible:outline-none` sans ring, `ui/sheet.tsx:78 focus:` au lieu de `focus-visible:`, `ChatMessage:30 hover:` sans `:focus-visible`, `DocumentsPage:86 dropzone` sans `focus-within:` — **P2**
- **Problème :** suppressions sans remplacement ou indications souris seules.
- **Pourquoi :** `focus-visible:ring-*` exigé ; `:focus-visible` > `:focus`.
- **Impact :** focus clavier faible/invisible par endroits alors que `globals.css:220` est bon par défaut.
- **Correction :** ajouter `focus-visible:ring-2` ciblés, `focus-within:ring-2` dropzone, uniformiser `sheet` en `focus-visible:`.

### C-15 — `MessageRenderer.tsx:170/29` + `MermaidBlock.tsx:50` + `MathBlock.tsx:41` — Zones scroll non focusables clavier — **P2**
- **Problème :** `div/pre overflow-x-auto` tableaux/code/mermaid/math sans `tabIndex={0}` ni `aria-label`.
- **Pourquoi :** contenu scrollable horizontal doit être atteignable clavier.
- **Impact :** tableaux larges/code inaccessibles au clavier.
- **Correction :** `tabIndex={0} role="region" aria-label="Tableau défilant"` (ou code/diagramme) sur conteneurs.

### C-16 — `MessageRenderer.tsx:171-185` — `th` sans `scope` — **P2**
- **Problème :** `th` sémantique mais sans `scope="col"`.
- **Pourquoi :** association en-têtes/cellules exigée.
- **Impact :** lecture tableau dégradée.
- **Correction :** `scope="col"` (+ `scope="row"` si 1ère colonne en-tête).

### C-17 — Icônes décoratives non masquées : `ChatPage:2/158/193` (`Brain/Plus`), `PersonaModal:196 RefreshCw`, `ChatMessage:32 Info`, `AppSidebarEnseignant:111 GraduationCap` — **P2**
- **Problème :** `aria-hidden` présent sur `MimeIcon/DocumentsPage:128` et `EtagereSection:47` mais absent ici (ou `aria-hidden` sans `="true"` sur `AppSidebar:61/107`).
- **Pourquoi :** décoratives exigent `aria-hidden="true"`.
- **Impact :** bruit AT (« image Brain »).
- **Correction :** `aria-hidden="true"` systématique + `focusable="false"`, normaliser `aria-hidden` (pas de forme bare).

### C-18 — Touch : `EtagereFiltres:44-90` (`h-9/h-8`), `ShareQuiz/ShareFiche label px-2 py-1.5` (~32px), `radio/checkbox size-4` 16px, `objectifs:113 select` ~18px, `sidebar h-8/w-4` — **P2**
- **Problème :** sous 44px sans compensation globale (`touch-action` grep 0, `after:-inset-2` seulement sidebar).
- **Pourquoi :** cibles tactiles ≥44px (ou zone élargie).
- **Impact :** fautes de frappe mobile, notamment statut objectif et radios partage.
- **Correction :** `min-h-11` sur contrôles critiques, `after:-inset-2` ou padding là où la densité l'exige, garder pastilles 16px mais hit-target via `label` (déjà ok quiz passage).

### C-19 — `routes/_public/mot-de-passe-oublie.tsx:62` + `JoinEspaceModal.tsx:71 autoFocus` — **P3**
- **Problème :** `autoFocus` mobile non exclu.
- **Pourquoi :** `autoFocus` avec parcimonie, desktop seul, un seul input primaire ; ouvre le clavier iOS et déplace le layout.
- **Impact :** clavier intempestif mobile, scroll sauté.
- **Correction :** `autoFocus` seulement si `matchMedia(pointer:fine)` / `use-mobile.ts` desktop, sinon focus manuel sur erreur.

---

## 2. UX (parcours, formulaires, états, navigation, feedback)

### U-01 — `MembresPage.tsx:256-264` + `MembresPageEnseignant.tsx:277-285` — Retirer un membre sans confirmation — **P0**
- **Problème :** `removeMembre.mutate(m.userId)` direct, vs `window.confirm` pour quitter/supprimer groupe et `Dialog` pour rejet fiche.
- **Pourquoi :** action destructive/irréversible sans confirmation ni undo.
- **Impact :** exclusion accidentelle, perte d'accès, conflit enseignant/élève.
- **Correction :** `Dialog` de confirmation (comme `fiches-a-valider:168-198` + `parametres confirmDelete`) avec nom du membre + conséquences + `Annuler` focus par défaut ; ou undo window.

### U-02 — `routes/_app/objectifs.tsx:199-208` — Supprimer un rappel sans confirmation — **P1**
- **Problème :** `deleteRappel.mutate(r.id)` direct sur `Supprimer ghost`.
- **Pourquoi :** destructif sans confirmation.
- **Impact :** perte rappel planifié d'un clic.
- **Correction :** même `Dialog` confirmation (ou undo). Rendre le bouton plus explicite (`Supprimer le rappel` + `aria-label`).

### U-03 — `routes/_app/espaces/$spaceId/quiz/$quizId/take.tsx:64` — Écran blanc si quiz vide — **P0**
- **Problème :** `if (!quiz || questions.length===0) return null`.
- **Pourquoi :** empty state non géré, contrairement au reste (étagère, documents).
- **Impact :** page blanche incompréhensible si génération vide/corrompue.
- **Correction :** empty dédié : « Ce quiz ne contient aucune question » + causes + CTA `Retour au quiz / Régénérer` + lien support ; logger `parseQuizQuestions` vide.

### U-04 — `components/shared/ChatPage.tsx:35-80` — Chargement confondu avec vide + blanc `return null` — **P1**
- **Problème :** `!conversations || length===0` affiche `Aucune conversation` même pendant fetch (`useConversations` sans `isLoading/isError`) ; `if (!activeId) return null` rend blanc.
- **Pourquoi :** états non distingués.
- **Impact :** faux vide au chargement lent, flash puis contenu ; blanc si `activeId` nul malgré conversations.
- **Correction :** brancher `isLoading/isError` : skeleton liste + thread, erreur + `Réessayer`, vide seul si `data!==undefined && length===0` ; jamais `return null` (repli `Sélectionne une conversation`).

### U-05 — `components/shared/DocumentsPage.tsx:36` + `routes/_app/tableau-de-bord.tsx:20-46` + `routes/_app/objectifs.tsx:46-49` — `isLoading/isError` ignorés — **P1**
- **Problème :** `useDocuments` sans `isLoading` (ni liste ni empty si `undefined`), `dashboard &&` sans chargement/erreur (vide partiel), objectifs/badges sans chargement/erreur.
- **Pourquoi :** données préchargées via `loader` mais refresh/direct-access repassent par vide.
- **Impact :** blancs partiels, incompréhension.
- **Correction :** skeletons ciblés (`EtagereGrid:23` déjà bon à généraliser), erreurs + `Réessayer` (`query.refetch`), ne pas conditionner des blocs entiers à `data &&`.

### U-06 — Formulaires : erreurs globales brutes, pas d'inline par champ ni focus — `LoginForm:36-41`, `RegisterForm:54`, `creer-espace:20-26`, `CreateEspaceModal:29-43`, `mot-de-passe-oublie:21-28/48-73`, `GenerateQuiz/GenerateFiche/Share*:75/92/215` — **P1**
- **Problème :** `login.error.message` brut, `isError` parfois jamais rendu, `JoinEspaceModal:24-78` seule erreur typée mais sans `aria-invalid/describedby/focus`, `PersonaModal:173-182` sans live.
- **Pourquoi :** erreurs exigent inline près du champ + focus première erreur + next step, pas message API brut.
- **Impact :** l'utilisateur ne sait pas quel champ corriger ni comment ; échec silencieux mot de passe oublié.
- **Correction :** mapper codes (404/409/validation) en messages + actions (« Vérifie le code auprès de ton enseignant », « Réessaie »), `aria-invalid + aria-describedby`, `ref.focus()` première erreur au submit, ne jamais `setEnvoye(true)` en échec (`mot-de-passe-oublie:21-28`).

### U-07 — `routes/onboarding/bienvenue.tsx:18-25` + `routes/_public/connexion.tsx:10-12` — Onboarding : erreurs invisibles + redirect perdu — **P1**
- **Problème :** `updateProfile.isError` jamais affiché (boutons restent `Chargement…`), `connexionSearchSchema {redirect}` validé mais jamais consommé.
- **Pourquoi :** pas de feedback échec, pas de reprise post-login.
- **Impact :** clic sans effet apparent ; retour à `/` au lieu de la page demandée.
- **Correction :** erreur + `Réessayer` près des boutons rôle, `redirect` consommé après `login onSuccess` (`navigate({to: redirect ?? '/'})`), garder `Passer cette étape` en secondaire.

### U-08 — Chat : thread vide, envoi invisible, monoligne — `ChatThread.tsx:26-46`, `ChatInput.tsx:20-38` — **P1**
- **Problème :** `messages=[]` = blanc + input seul (pas de vide guidé), `disabled` seul pendant envoi (pas de `Envoi…`/`aria-busy`), `input` monoligne (pas de multiline `Shift+Enter`), `scrollIntoView smooth:21` sans reduced-motion (le hook `use-chalk-reveal:18` le gère, pas le thread).
- **Pourquoi :** feedback + densité + accessibilité motion.
- **Impact :** premier usage froid, questions longues pénibles, motion imposée.
- **Correction :** empty thread (« Pose ta première question… + exemples »), état `Envoi en cours…` + `aria-busy`, `textarea auto-resize` avec `Enter=envoyer / Shift+Enter=saut`, `behavior: matchMedia(reduced-motion) ? 'auto':'smooth'`.

### U-09 — État non persisté en URL : `EtagereGrid:21`, `ChatPage:39`, `tableau-de-bord:17`, `objectifs:43`, `fiches-a-valider:207`, `take:29-31` — **P1**
- **Problème :** filtres/recherche/tri/owner, conversation active, espace actif, `currentStep/answers/submitted` en `useState` seul.
- **Pourquoi :** filtres/tabs/pagination/état stateful doivent vivre en query params (deep-link).
- **Impact :** reload = perte filtres/progression quiz, non partageable (« regarde ce filtre » impossible), refresh quiz = perte réponses.
- **Correction :** `nuqs` ou `validateSearch` TanStack pour filtres/espace/conversation/`?q=` quiz (avec persistance locale en complément pour réponses, pas en remplacement) ; garder `useState` pour draft/hover.

### U-10 — `routes/_app/espaces/$spaceId/parametres.tsx:53-58/124-127` + `MembresPage:193-230` — Copie code sans annonce ni fallback — **P2**
- **Problème :** `Copié ✓` en texte seul sans `aria-live`, `clipboard.writeText` sans `try/catch` (idem `CodeBlock:11-15`).
- **Pourquoi :** feedback async doit être annoncé ; clipboard peut échouer (permissions).
- **Impact :** AT ne confirme pas la copie ; échec silencieux.
- **Correction :** `role="status"` + `try/catch` + repli (sélection manuelle / `execCommand`) + message d'échec.

### U-11 — Pas de garde saisie non sauvegardée : `parametres`, `espace/parametres`, `take`, `creer-espace` (grep `beforeunload/useBlocker` 0) — **P2**
- **Problème :** navigation sidebar quitte sans avertir avec draft (`PersonaModal draft`, `take answers`, formulaires).
- **Pourquoi :** `beforeunload` / router guard exigés.
- **Impact :** perte réponses quiz / persona / profil.
- **Correction :** `useBlocker` TanStack quand `dirty && !submitted`, `beforeunload` pour refresh/fermeture, `Dialog` « Quitter sans enregistrer ? ».

### U-12 — `EtagereGrid` + `SpineCard.tsx:20` — N+1 requêtes + listes non paginées — **P2**
- **Problème :** chaque `SpineCard` fait `useConversations(space.id)` ; `fiches-a-valider:278-282 map` lourd (chaque ligne porte `useFiche + Dialog`), `mes-fiches/quiz/fiches map` intégraux, `TeacherDashboardBlocks:65 slice(0,10)` sans total/voir-plus.
- **Pourquoi :** perf = UX ; listes >50 exigent virtualisation/pagination.
- **Impact :** étagère lente avec N espaces, page validation lourde, troncature silencieuse.
- **Correction :** agréger conversations (endpoint ou `useQueries` parent déjà esquissé dans `_app/index:27-34` à généraliser), pagination + `Voir plus` + compte total, `content-visibility:auto` / virtualisation si >50.

### U-13 — Copy incohérente : `tu/vous` (`EtagereGrid:28 tes` vs `ShareQuizModal:48 Choisissez`), `Générer un Quiz` (`GenerateQuizModal:101`, `quiz/index:25`) vs `Générer une fiche`, `MembresPage:64 Depart…` sans accent, `ValidationSection:56` passif, `Bientôt disponible` sans date (`tableau-de-bord:131`, `objectifs:251`), erreurs sans next step — **P2**
- **Problème :** voix, casse, accents, passif, impasses.
- **Pourquoi :** voix active 2e personne, `Title Case` headings, erreurs avec fix exigés.
- **Impact :** confiance/ton amateur, blocages sans issue.
- **Correction :** fixer `tu` partout (produit scolaire), `Générer un quiz`, `Départ…`, voix active, chaque erreur + action (`Réessayer`, `Vérifie…`, `Contacte…`), dater ou retirer `Bientôt`.

---

## 3. Visuel (hiérarchie, cohérence, lisibilité, typo, espacements, densité)

### V-01 — `AppSidebar.tsx:55` + `ChatPage.tsx:84-94/183-186` — Ardoise hors chat + mélange de surfaces — **P1**
- **Problème :** sidebar `surface-ardoise` permanente alors que `globals.css:90-96` la réserve au wrapper chat ; `ChatPage` superpose `surface-ardoise` + bandeau `bg-papier-bg` + rail `bg-papier-bg` + `bg-muted/secondary` (`ChatInput:22`, `CodeBlock:18`, `MessageRenderer:175/206`).
- **Pourquoi :** contrat : jamais mélanger les surfaces sur un même écran ; sidebar doit suivre la surface courante.
- **Impact :** sidebar sombre sur pages Papier = rupture, chat illisible par îlots (chips Papier ok, code/input muted non prevista).
- **Correction :** sidebar en tokens sémantiques (`bg-sidebar`) qui suivent `.surface-ardoise` seulement sur chat, ou 2 variantes assumées (Ardoise chat / Papier ailleurs) ; sur chat, décider : tout Ardoise (input/code/table en `ardoise-raised/border`) avec îlots Papier seulement pour citations/popovers (déjà documenté `globals.css:103`).

### V-02 — `SpaceLayout.tsx:26-53` — En-tête sans garde-fous + onglets non scrollables — **P1**
- **Problème :** `h1 {space?.name}` vide en chargement (pas de skeleton), description sans `line-clamp`, zéro `truncate/min-w-0`, `nav flex gap-1 px-6` sans `overflow-x-auto/flex-wrap` pour 5-6 onglets.
- **Pourquoi :** conteneurs doivent gérer long + `min-w-0` + responsive.
- **Impact :** titre vide puis saut, noms longs qui poussent/débordent à 360px, onglets coupés.
- **Correction :** skeleton `h1`, `truncate + min-w-0` + `title`, `line-clamp-2` description, `nav overflow-x-auto + whitespace-nowrap + scroll-mt`, `px-4 sm:px-6`.

### V-03 — Hover sans équivalent focus : `QuizCard.tsx:22 hover:bg-papier-bg`, `SpineCard.tsx:29 hover:-translate-y-1/shadow`, `fiches/index:41 hover:bg-secondary`, `DocumentsPage:86-89 hover:` — **P2**
- **Problème :** feedback souris seul.
- **Pourquoi :** états interactifs doivent augmenter le contraste au hover/active/focus, pas seulement hover.
- **Impact :** clavier ne perçoit pas la sélection.
- **Correction :** `focus-visible:ring-2 + focus-visible:bg-…` miroirs du `hover:`, `active:` légèrement plus contrasté, soulignement titre au `hover/focus` sur cartes-liens.

### V-04 — Typo : 0 `tabular-nums` (sauf `sidebar:589`), 0 `text-balance/pretty` (sauf `tooltip`), `…` incohérent — **P2**
- **Problème :** scores `%`, compteurs, dates en `font-mono` sans chasse tabulaire (`QuizResultsPanel:21-26`, `tableau-de-bord:53-120`, `TeacherDashboardBlocks:85-103`) ; titres `QuizCard:29 h3 text-xl`, `FicheCard:30`, `QuizQuestionCard:25` sans `balance` ; placeholders majoritairement sans `…` (`creer-espace:33/37`, `JoinModal:73`, `objectifs:132/229`, `parametres:99-113`) alors que `ChatInput:27`, `EtagereFiltres:37`, `CreateEspaceModal:65` sont conformes.
- **Pourquoi :** `tabular-nums` pour comparaisons, `balance/pretty` anti-veuves, `…` + exemple pour placeholders.
- **Impact :** chiffres qui sautent, titres à mot seul, aides inconsistantes.
- **Correction :** `tabular-nums` sur scores/compteurs/`%`, `text-balance` headings + `text-pretty` paragraphes `leading-relaxed`, uniformiser placeholders `ex : …` avec `…`.

### V-05 — Badges/statuts incohérents — **P2**
- **Problème :** `FicheValidationBadge:13/20` (`Validée succes / À revoir attention`, tampon jamais rouge) vs `fiches-a-valider:96-103` (`REJETEE erreur`) vs `fiches/index:32-41` ; `quiz/$quizId:80-90` (`>=80 succes / >=50 default sombre / else attention`) ambigu ; `objectifs:32-35 ABANDONNE erreur` alarmiste ; `objectifs:196-198` date déguisée en `StatutBadge`.
- **Pourquoi :** même état = même couleur ; `&` / casse / voix cohérentes.
- **Impact :** rejet perçu tantôt avertissement tantôt erreur, 50-80% illisible, abandon anxiogène.
- **Correction :** référentiel unique (`VALIDEE/succes, EN_ATTENTE/secondary, REJETEE/erreur, À revoir/attention`), quiz `>=80 succes / >=50 attention / else erreur` (ou `secondary`), abandon en `secondary`, dates en texte mono (pas badge).

### V-06 — Responsive : paddings fixes + grilles sans paliers — **P2**
- **Problème :** `DocumentsPage:74 p-6`, `tableau-de-bord:25 p-8`, `objectifs:78 p-8`, `SpaceLayout:26 px-6`, `ChatThread:26/41 max-w-2xl px-6` identiques mobile/desktop ; `DocumentsPage` 0 breakpoint ; `accueil:78 sm:grid-cols-3` ok mais `FicheCard` + CTA `flex gap-3` sans `flex-wrap`.
- **Pourquoi :** `flex/grid` + paliers, pas de `overflow-x` subi.
- **Impact :** marges énormes à 360px, lignes trop longues desktop chat, CTA qui débordent.
- **Correction :** `p-4 sm:p-6 lg:p-8`, `px-4 sm:px-6 lg:px-8`, chat `max-w-2xl` + `px-4 sm:px-6`, CTA `flex-wrap`, tester 360/768/1280.

### V-07 — `EtagereGrid:23-28` + `PersonaModal:106/143` + `ChatThread:27` — Chargements en texte seul — **P2**
- **Problème :** `Chargement…` en `p` alors que `Skeleton` existe (`ui/skeleton.tsx`, `SidebarMenuSkeleton`).
- **Pourquoi :** skeleton = hiérarchie perçue, moins de CLS.
- **Impact :** flash texte, sauts de layout.
- **Correction :** skeletons (cartes étagère, lignes documents, thread, dashboard) + `aria-busy + role=status`.

### V-08 — `routes/_public/accueil.tsx:71-85` — Landing : eyebrow en `p`, `h1→h3`, CTA non hiérarchisés au focus — **P3**
- **Problème :** `Exemple de fiche générée` en `p mono` au lieu de `h2`, piliers en `h3` sans `h2`, 2 `Button lg` côte à côte sans `flex-wrap`.
- **Pourquoi :** hiérarchie + responsive de base.
- **Impact :** plan AT plat, débordement 360px.
- **Correction :** `h2` section exemple + piliers, `flex-wrap justify-center`, `text-balance` `h1`.

---

## 4. Améliorations secondaires (P3 sauf mention)

### S-01 — Dates : centraliser `Intl` + `<time>` + relatif
- 28× `toLocaleDateString('fr-FR')` disparates (`JJ/MM/AAAA` vs `+ heure`, `SpineCard:27/35` redondant + année absente, `TeacherDashboardBlocks:94` sans année, casses `membre depuis le / Généré le / Date limite :`).
- Correction : helper `formatDate/formatDateTime` (`Intl.DateTimeFormat('fr-FR')`), `<time dateTime={iso}>`, relatif (« il y a 2 j ») pour conversations/activités, labels uniformisés.

### S-02 — `index.html:5/12-17` + `globals.css` — Perf/polish fonts & meta
- Viewport sans `viewport-fit=cover`, sans `<meta name="theme-color" content="…papier-bg">`, sans `color-scheme` (grep 0) ; `preconnect` ok mais pas de `preload as=font`, self-host `@fontsource` seulement en commentaire.
- Correction : `viewport-fit=cover + theme-color`, `color-scheme: light` (pas de dark applicatif, Ardoise n'est pas un thème), migrer en self-host comme prévu, `font-display:swap` conservé.

### S-03 — Layout : `min-h-screen` vs `min-h-svh`, safe-area, overscroll, scroll-margin
- `onboarding/route:9` + `_public/route:27 min-h-screen` vs `h-svh/min-h-svh` ailleurs (jump iOS) ; 0 `env(safe-area-inset-*)` (`sheet bottom` + sidebar mobile), 0 `overscroll-behavior:contain` (`dialog/sheet/dropdown/sidebar`), 0 `scroll-mt` (prévoir si futur sticky, aujourd'hui aucun `sticky` donc pas de masquage focus).
- Correction : uniformiser `svh`, `pb-[env(safe-area-inset-bottom)]` sheets/drawers, `overscroll-contain + max-h + overflow-y-auto` dialogs, `scroll-mt-16` ancres.

### S-04 — `MessageRenderer:217`, `CodeBlock:23`, `Mermaid/Math` — Liens externes, copies, diagrammes
- Liens `target=_blank` texte `link[1]` sans signal externe ni `aria-label` ; `MermaidBlock:51 div ref + innerHTML` sans `role="img" aria-label` + erreur seule en `pre` ; `MathBlock:40-44` sans équivalent ; `CodeBlock` swap sans live.
- Correction : icône `ExternalLink aria-hidden + sr-only (nouvel onglet)`, `role="img" aria-label={chart.slice(0,140)}` + `<details><summary>Code source</summary><pre>` pour mermaid/math, `aria-live` copies.

### S-05 — Emojis/brand : `🌱🧠💬📄⚠️🔒★` (`AppSidebar:61`, `ChatPage:109`, `_app/index:68/82`, `FicheCard:53`, `objectifs:170`)
- Correction : `aria-hidden="true"` + texte adjacent déjà présent, `translate="no"` sur `TsimokaAI`, éviter emoji comme seul signal (badges déjà texte + icône Lucide, à généraliser).

### S-06 — Hydration/prefetch : `main.tsx:25`, `MessageRenderer:44`
- `autocomplete` en commentaire seulement (pas d'hydratation) ; `img src=''` évité via S-04 ; `defaultPreload:intent` déjà bon, ajouter `prefetch` explicite sur CTA étagère si besoin. Pas de `suppressHydrationWarning` abusif constaté — ne rien changer.

---

## 5. Synthèse — les plus importants

**À traiter en premier (risque + fréquence) :**
1. **Destructif sans filet (U-01 P0 + U-02 P1)** — `Retirer membre` et `Supprimer rappel` en 1 clic. Uniformiser sur le bon pattern `Dialog + explications + Annuler` de `fiches-a-valider`.
2. **Inaccessibilité clavier des parcours cœur (C-01/C-02 P0)** — `ChatInput` sans nom + focus effacé, dropzone `div` non focusable. Rend chat et documents inutilisables au clavier/AT.
3. **Formulaires sans nom ni aide (C-03/C-05 P0-P1 + U-06 P1)** — passwords, commentaires, annotations, objectifs en placeholder-only, zéro `autocomplete`, erreurs brutes sans focus. Bloque auth/compte et dégrade tout le reste.
4. **États qui mentent (U-03 P0 + U-04/U-05 P1)** — quiz blanc, chat faux-vide + blanc, dashboards partiellement vides, 0 `aria-live`. L'utilisateur ne sait jamais s'il charge, échoue ou est vide.
5. **Surfaces hors contrat (V-01 P1)** — sidebar Ardoise permanente + chat multi-surfaces. Trancher : sidebar sémantique qui suit la surface, chat 100% Ardoise (input/code/table en `raised/border`) sauf îlots Papier documentés.
6. **Navigation non partageable (U-09 P1 + C-10/U-11 P1-P2)** — tout en `useState`, `<a>/location.href`, pas de garde dirty. Filtres et progression quiz perdus, liens qui rechargent.
7. **Hiérarchie + tables + progress (C-09/C-13 P1-P2)** — `Progress` sans valeur, `h1→h3`, `th` sans `scope`, zones scroll non focusables. Navigation AT et tableaux quiz/pédagogiques dégradés.
8. **Quick-wins sûrs :** `aria-label` conversation collapsed (C-06), `Link` retour (C-10), `autocomplete` (C-05), `scope` + `tabIndex region` (C-15/C-16), `aria-hidden` icônes (C-17), `tabular-nums/balance/…` (V-04), skeletons (V-07), `theme-color/svh/safe-area` (S-02/S-03).

**Fichiers à revoir en priorité :** `ChatInput.tsx`, `DocumentsPage.tsx`, `ChatPage.tsx`, `SpaceLayout.tsx`, `MembresPage*.tsx`, `LoginForm/RegisterForm.tsx`, `parametres.tsx`, `objectifs.tsx`, `ValidationSection/AnnotationsSection.tsx`, `take.tsx`, `tableau-de-bord.tsx`, `EtagereGrid/Filtres.tsx`, `Generate*Modal/Share*Modal.tsx`, `MessageRenderer/CodeBlock/Mermaid/MathBlock.tsx`, `ui/progress/sidebar/dialog/dropdown/tabs.tsx`, `globals.css`, `index.html`, `main.tsx`, `__root/_app/enseignant/_public/onboarding routes`.

*Audit statique — à compléter si besoin par tests clavier (Tab/Shift+Tab/Enter/Espace/Échap), lecteur d'écran (NVDA/VoiceOver), 360px + clavier iOS/Android, et contrastes mesurés (OKLCH→sRGB).*
