# Plan d'amélioration frontend — TsimokaAI

**Date :** 2026-09-27
**Sources :** `docs/ui-audit/audit-muse.md` + `docs/ui-audit/audit-independent.md` + vérification code `frontend/src` + skill `web-design-guidelines` + `vercel-labs/web-interface-guidelines`
**Périmètre :** UI/UX/a11y uniquement. Contrats API et logique métier inchangés.
**Méthode :** chaque point confirmé/infirmé par lecture code. Doublons fusionnés. Suggestions non justifiées écartées. Désaccords tranchés sur code.

## 0. Vérification — écartés / downgradés

Écartés (faux positifs, ne pas traiter) :
- `accueil.tsx:63-68 Button asChild > Link` (independent #3 P0) — **INFIRMÉ** : pattern Slot Radix correct, SPA + clavier natifs. Ne rien changer.
- `PersonaModal.tsx:164-166 textarea sans id` (independent #34) — **INFIRMÉ** : `<Label htmlFor="persona-edit"> + <textarea id="persona-edit">` correctement associé.
- `accueil.tsx &eacute;` (independent #16) — **INFIRMÉ** : apostrophes `’` unicode directes. `_public/route.tsx:45 &copy;/&eacute;` rend correctement en JSX, pas de bug.
- `globals.css:220 dashed` (independent #36) — préférence esthétique, base `:focus-visible 2px dashed var(--attention)` conforme et visible. Garder.
- `main.tsx prefetch manquant` — **INFIRMÉ** : `defaultPreload:intent + ensureQueryData` existe.
- `Input.tsx defaut autocomplete=off + warning TS` (independent #2) — reformulé : pas de défaut magique, mettre `autocomplete/name` explicites par champ (Lot 2).
- `window.confirm = bon pattern` (independent « ce qui fonctionne ») — **inversé** : `window.confirm` natif bloque AT, sans style/focus. À remplacer par `Dialog` (Lot 1).
- `font-mono = tabular` (independent « ce qui fonctionne ») — surévalué : `font-mono` seul ≠ `tabular-nums`. Corriger (Lot 6).
- `shadow-xs non standard` (independent #31), `avatar hash couleur` (#17) — dette DX / préférence, P3 ou écarté si temps manque.

Downgradés :
- `AppSidebar Bell sans aria-label` (independent #4 P0) → **P1** : bloque ce contrôle AT, pas toute l'app.
- `SidebarRail tabIndex=-1` (independent #5 P0) → **P2** : alternative clavier existe (`Ctrl+B` + Trigger). Rendre label FR + garder décoratif ou focusable, pas bloquant.
- `JoinEspaceModal toUpperCase onChange` (independent #6 P0) → **P1** : casse IME/composition + curseur, avéré `JoinEspaceModal.tsx:76`, mais non bloquant FR principal.
- `button outline-none double ring` (independent #7) → **P2** : uniformisation focus, pas cassé.

## 1. P0 — bloquant / UI cassée

| ID | Problème vérifié | Fichier |
|----|------------------|---------|
| P0-1 | Chat sans nom + focus effacé | `features/chat/components/ChatInput.tsx:24-29` `focus:outline-none` sans remplacement, sans label/id/aria-label |
| P0-2 | Dropzone non clavier | `components/shared/DocumentsPage.tsx:81-108` `div onClick/onDrop` sans role/tabIndex/keyboard, `input hidden` |
| P0-3 | Champs critiques placeholder-only | `features/fiches/components/ValidationSection.tsx:90-95`, `routes/_app/parametres.tsx:97-117` (x2 variantes enseignant), `AnnotationsSection.tsx:50-60` |
| P0-4 | Destructif sans filet | `components/shared/MembresPage.tsx:260` + `MembresPageEnseignant.tsx` `removeMembre.mutate` direct |
| P0-5 | Écran blanc quiz vide | `routes/_app/espaces/$spaceId/quiz/$quizId/take.tsx:64` `return null`, idem `quiz/$quizId/index.tsx:22`, `parametres.tsx:60` |

## Lots

### Lot 1 — P0 : parcours cœur clavier + garde-fous destructifs
**Objectif :** rendre chat/documents utilisables clavier/AT + supprimer perte d'accès accidentelle + blanc quiz.
**Fichiers :** `ChatInput.tsx`, `DocumentsPage.tsx`, `MembresPage.tsx`, `MembresPageEnseignant.tsx`, `take.tsx`, `quiz/$quizId/index.tsx`, `ValidationSection.tsx`, `AnnotationsSection.tsx`, `parametres.tsx` (app+enseignant).
**Modifications :**
- `ChatInput` : `id=chat-input + <label sr-only>Écris ta question…` ou `aria-label`, `focus-visible:ring-2` au lieu de `focus:outline-none`, garder placeholder avec `…`, `aria-busy` pendant envoi.
- Dropzone : `<button type=button aria-label=Déposer des fichiers aria-describedby=formats>` ou `div role=button tabIndex=0 onKeyDown Enter/Espace + focus-visible:ring-2 + focus-within:ring-2`, `input sr-only` pas `hidden`.
- Passwords/commentaire/annotations : `Label htmlFor + Input id` visibles ou `sr-only`, `aria-invalid + aria-describedby` prêt pour Lot 2.
- `Retirer membre` : `Dialog` confirmation comme `fiches-a-valider:168-198` (nom + conséquences + Annuler focus défaut), pas `window.confirm`.
- `take.tsx` : empty dédié « Aucune question — Retour/Régénérer », jamais `return null`.
**Priorité :** P0.
**Risques :** focus chat modifié → tester Tab/lecteur ; Dialog retire → tester Annuler/Échap/focus trap ; dropzone button → tester drag + clavier + SR.

### Lot 2 — P1 : formulaires nommés + erreurs actionnables
**Objectif :** autofill/claviers mobiles + erreurs inline avec next step + onboarding fiable.
**Fichiers :** `features/auth/LoginForm.tsx`, `RegisterForm.tsx`, `routes/_app/parametres.tsx`, `enseignant/parametres.tsx`, `routes/_app/objectifs.tsx`, `MembresPage*.tsx`, `CreateEspaceModal.tsx`, `JoinEspaceModal.tsx`, `onboarding/bienvenue.tsx`, `creer-espace.tsx`, `routes/_public/connexion.tsx`, `mot-de-passe-oublie.tsx`, `GenerateQuizModal.tsx`, `GenerateFicheModal.tsx`, `ShareQuizModal.tsx`, `ShareFicheModal.tsx`, `PersonaModal.tsx`, `ui/input.tsx`.
**Modifications :**
- `autocomplete/name/inputmode` explicites : `email→email`, `login pwd→current-password`, `register/nouveau→new-password`, `displayName→name`, code → `one-time-code + inputMode=text autoCapitalize=characters spellCheck=false`, non-auth (`subjectTag`) → `autocomplete=off`. Ne pas mettre de défaut magique dans `Input`.
- `JoinEspaceModal:76` : `text-transform:uppercase` CSS + `toUpperCase` au submit (`use-join-espace.ts:17` garder), pas en `onChange` (fix IME).
- Erreurs : inline près du champ + `aria-invalid/describedby` + `focus()` première erreur, mapper codes 404/409/validation (« Vérifie le code auprès de ton enseignant », « Réessaie »), ne jamais `setEnvoye(true)` en échec (`mot-de-passe-oublie:24-26`), afficher `updateProfile.isError + Réessayer` (`bienvenue`), consommer `redirect` après login (`use-login.ts:25 navigate redirect ?? /`).
- `Generate/Share` : erreur typée + live (prépare Lot 3), radios/checkbox : un seul `label>input` natif ≥44px, supprimer `button>input` (`GenerateQuizModal:282-295`, `GenerateFicheModal:241-249`), `fieldset/legend` ou `role=radiogroup + label`.
**Priorité :** P1 (P0 partiel passwords déjà Lot 1).
**Risques :** autofill navigateur change comportement → tester gestionnaires mdp ; focus erreur → tester SR ; redirect → tester deep-link post-login, pas de boucle.

### Lot 3 — P1 : états loading/empty/error + live regions
**Objectif :** l'utilisateur sait toujours s'il charge/échoue/est vide, AT notifié.
**Fichiers :** `components/shared/ChatPage.tsx:35-80`, `DocumentsPage.tsx:36`, `routes/_app/tableau-de-bord.tsx`, `enseignant/tableau-de-bord.tsx`, `routes/_app/objectifs.tsx`, `features/chat/ChatThread.tsx`, `EtagereGrid.tsx`, `PersonaModal.tsx`, `TeacherDashboardBlocks.tsx`, `ui/skeleton.tsx`, `ui/progress.tsx`, `features/chat/CodeBlock.tsx`, `routes/_app/espaces/$spaceId/parametres.tsx`.
**Modifications :**
- Brancher `isLoading/isError` : skeleton liste+thread (généraliser `EtagereGrid:23`), erreur + `Réessayer (refetch)`, vide seul si `data!==undefined && length===0`. Supprimer `if(!activeId) return null` → repli « Sélectionne une conversation ».
- `role=status aria-live=polite` chargements/succès, `role=alert` erreurs, `aria-live` copies (`Copié`), `CodeBlock` swap, badges notif (`AppSidebar:170-172 aria-label=X rappels + live`).
- `progress.tsx:6-17` : retransmettre `value/max` à `Root` (fix `aria-valuenow` quiz `take:92`).
- `ChatThread` : empty guidé (« Pose ta première question + exemples »), `Envoi en cours… + aria-busy`, `textarea auto-resize Enter=envoyer/Shift+Enter=saut` (ou garder input + doc), `scrollIntoView behavior=reduced-motion?auto:smooth`.
- Skeletons ciblés étagère/documents/thread/dashboard + `aria-busy`.
**Priorité :** P1.
**Risques :** skeletons → CLS si tailles fausses ; live trop verbeux (streaming chat) → polite + throttle ; progress → tester SR.

### Lot 4 — P1 : navigation SPA + landmarks + hiérarchie + surfaces
**Objectif :** SPA sans reload, navigation clavier rapide, plan AT cohérent, contrat Papier/Ardoise respecté.
**Fichiers :** `components/shared/SpaceLayout.tsx:27-53`, `MembresPage*.tsx:52/58`, `routes/__root.tsx`, `routes/_app/route.tsx`, `routes/enseignant/route.tsx`, `routes/_public/route.tsx`, `components/ui/sidebar.tsx:282-309`, `ChatPage.tsx:84-94/116-128/191-198`, `tableau-de-bord.tsx`, `objectifs.tsx`, `EtagereFiltres.tsx`, `EtagereGrid.tsx`, `fiches-a-valider.tsx`, `take.tsx`, `accueil.tsx`, `FicheCard.tsx`, `QuizQuestionCard.tsx`, `QuizResultsPanel.tsx`, `parametres.tsx`, `ui/card.tsx`, `styles/globals.css`, `ChatMessage.tsx`.
**Modifications :**
- `SpaceLayout:27 <a href>` → `Link to`, `window.location.href` → `navigate({to})`.
- Skip-link « Aller au contenu » dans `__root` → `main id=main`, `nav aria-label` (« Principale », « Onglets espace »), `SidebarRail` : label FR + soit focusable avec keyboard, soit `aria-hidden` + garantir `Trigger + Ctrl+B` documenté.
- Selects : `Label htmlFor + id` explicites (« Conversation active », « Espace actif », « Statut objectif »), `displayName` pas `userId.slice(0,8)`, hauteur ≥44px mobile. `ChatPage collapsed` : `aria-label=Nouvelle conversation` permanent.
- Headings : `h1` par page, sections `h2`, cartes `h3`, questions `h3` pas `h4`, score en `h2`, `CardTitle` paramétrable `h2/h3` ou `aria-labelledby`. Fix `tableau-de-bord h1→h3`, `accueil h1→h3 + eyebrow p→h2`, `connexion/inscription h2 orphelin`.
- Surfaces (contrat `globals.css:90-96`) : sidebar en tokens sémantiques qui suivent surface courante (Papier hors chat, Ardoise chat), chat 100% Ardoise (`input/code/table → ardoise-raised/border`), îlots Papier seulement citations/popovers documentés. Pas de `bg-muted/secondary` hors contrat.
- `SpaceLayout` header : skeleton `h1`, `truncate + min-w-0 + title`, `line-clamp-2` description, `nav overflow-x-auto whitespace-nowrap`, `px-4 sm:px-6`.
- URL state (léger, sans `nuqs` obligatoire) : `validateSearch` TanStack pour filtres étagère/espace/conversation/`?q=` quiz ; réponses quiz en local + garde, pas tout en URL.
**Priorité :** P1 (headings P2 mais groupé pour cohérence).
**Risques :** sidebar tokens → régression visuelle large, snapshot ; headings → CSS `h3` dépendant ; search params → partage/refresh à tester, éviter persistance sensible.

### Lot 5 — P2 : focus / touch / scroll / motion / guards
**Objectif :** focus toujours visible, cibles tactiles, scroll clavier, motion respectée, pas de perte saisie.
**Fichiers :** `ui/tabs.tsx:39`, `ui/dropdown-menu.tsx:38`, `ui/sheet.tsx:78`, `ui/dialog.tsx`, `ui/accordion.tsx:36`, `ui/sidebar.tsx:476-496`, `ui/button.tsx`, `ValidationSection.tsx:95`, `DocumentsPage.tsx:86`, `ChatMessage.tsx:30`, `QuizCard.tsx`, `SpineCard.tsx`, `features/chat/MessageRenderer.tsx:29/170`, `MermaidBlock.tsx`, `MathBlock.tsx`, `EtagereFiltres.tsx`, `Share*Modal.tsx`, `objectifs.tsx select`, `ChatThread.tsx:21`, `use-chalk-reveal.ts`, `parametres.tsx`, `PersonaModal.tsx`, `take.tsx`, `creer-espace.tsx`, `AppSidebar.tsx Bell`, `ChatMessage Info`, `dialog/sheet XIcon`, `onboarding/bienvenue.tsx`.
**Modifications :**
- Focus : `focus-visible:ring-2` ciblés, `focus-within:ring-2` dropzone, `sheet focus-visible:` pas `focus:`, miroir `hover: → focus-visible:` (`QuizCard`, `SpineCard`, `DocumentsPage`), `active:` légèrement plus contrasté. Résoudre double `ring + dashed` en gardant les deux sauf si test visuel impose `outline-offset` ajusté — ne pas supprimer `outline-none` sans remplacement.
- Touch : `min-h-11` contrôles critiques, `after:-inset-2` où densité l'exige, garder pastilles 16px mais hit-target via `label`.
- Scroll : `tabIndex=0 role=region aria-label` sur `overflow-x-auto` (table/code/mermaid), `th scope=col` (+row si besoin).
- Icônes : `aria-hidden=true focusable=false` systématique (`Plus/Brain/Info/XIcon/Chevron`), normaliser `aria-hidden` (pas forme bare).
- Motion : `prefers-reduced-motion` sur `scrollIntoView`, `accordion/sidebar/sheet/dialog` (`transition-[height,opacity,transform]` pas `transition-all`, `transform/opacity` seuls).
- Clipboard : `try/catch + role=status + fallback sélection manuelle`.
- Dirty : `useBlocker` TanStack quand `dirty && !submitted` + `beforeunload` + `Dialog Quitter sans enregistrer ?` (quiz réponses, persona draft, formulaires).
- `bienvenue` : `aria-pressed` ou `radiogroup`, `Bell` : `aria-label=Rappels`.
**Priorité :** P2.
**Risques :** focus global très visible → bruit visuel ; touch 44px → densité desktop ; `useBlocker` → blocage abusif si `dirty` mal calculé.

### Lot 6 — P2 : cohérence visuelle + responsive + typo
**Objectif :** même état = même couleur, lisible 360–1280, chiffres comparables, aides uniformes.
**Fichiers :** `FicheValidationBadge.tsx`, `fiches-a-valider.tsx:96-103`, `fiches/index.tsx`, `quiz/$quizId/index.tsx:80-90`, `objectifs.tsx:31-35/196-198`, `badge.tsx`, `QuizCard.tsx`, `FicheCard.tsx`, `QuizResultsPanel.tsx`, `tableau-de-bord.tsx`, `TeacherDashboardBlocks.tsx`, `DocumentsPage.tsx`, `SpaceLayout.tsx`, `ChatThread.tsx`, `EtagereGrid.tsx`, `accueil.tsx`, `SpineCard.tsx`, `EtagereSection.tsx`, `CitationChips.tsx`, `fiches/$ficheId.tsx`.
**Modifications :**
- Référentiel badges unique : `VALIDEE/succes, EN_ATTENTE/secondary, REJETEE/erreur, À revoir/attention`, quiz `>=80 succes / >=50 attention / else erreur`, abandon `secondary` pas `erreur`, dates en texte mono pas badge, `rounded` uniformisé.
- Responsive : `p-4 sm:p-6 lg:p-8`, `px-4 sm:px-6 lg:px-8`, chat `max-w-2xl px-4 sm:px-6`, CTA `flex-wrap`, grilles avec paliers, tester 360/768/1280. `SpineCard date 0.58rem→0.65rem min`.
- Typo : `tabular-nums` scores/compteurs/`%`/dates, `text-balance` headings + `text-pretty` paragraphes, placeholders `ex : …` avec `…` uniformisés, `…` pas `...`, `title` natif UUID → tooltip shadcn ou `[Source N]`, barre tag couleur + `title/tooltip` (pas couleur seule).
- Copy : fixer `tu` partout, `Générer un quiz`, accents, voix active 2e personne, chaque erreur + action, dater ou retirer `Bientôt disponible`.
- Perf listes : agréger `useConversations` parent (généraliser `_app/index:27-34`), pagination + `Voir plus + total`, `content-visibility:auto`/virtualisation si >50, `slice` avec compte.
**Priorité :** P2.
**Risques :** badges → changement sens perçu (valider métier) ; responsive → snapshots ; pagination → contrat API inchangé mais forme data à vérifier.

### Lot 7 — P3 : polish meta / layout / media
**Objectif :** détails secondaires, sans risque.
**Fichiers :** `index.html`, `styles/globals.css`, `onboarding/route.tsx`, `_public/route.tsx`, `MessageRenderer.tsx:217`, `CodeBlock.tsx`, `MermaidBlock.tsx`, `MathBlock.tsx`, `AppSidebar.tsx`, `ChatPage.tsx`, `FicheCard.tsx`, `objectifs.tsx`, `mot-de-passe-oublie.tsx`, `JoinEspaceModal.tsx`, `ui/tabs.tsx:28`.
**Modifications :**
- `index.html` : `viewport-fit=cover + theme-color=papier-bg`, `lang=fr dir=ltr` vérifié, garder `preconnect/display=swap`, self-host `@fontsource` comme prévu (pas `preload font` distant).
- Layout : uniformiser `svh` (`min-h-screen→min-h-svh`), `pb-[env(safe-area-inset-bottom)]` sheets/drawers, `overscroll-contain + max-h + overflow-y-auto` dialogs, `scroll-mt-16` ancres.
- Media : `figure` seulement si `url` truthy, `width/height` ou `aspect-ratio + loading=lazy decoding=async`, liens `target=_blank` + icône `ExternalLink aria-hidden + sr-only (nouvel onglet)` + `rel=noopener noreferrer`, mermaid/math `role=img aria-label + <details>Code source`.
- Emojis `🌱🧠💬📄⚠️🔒★` : `aria-hidden=true`, jamais seul signal, `translate=no` sur `TsimokaAI`.
- `autoFocus` : desktop seul (`pointer:fine`), sinon focus erreur.
- `tabs active border-tag-sciences → --ring/--primary`, dates → helper `Intl.DateTimeFormat + <time dateTime> + relatif`.
**Priorité :** P3.
**Risques :** quasi nuls, tester iOS notch + contraste `craie-muted/ardoise-bg` 4.5:1.

## Suivi d'implémentation

**Date :** 2026-09-27 — Lots 1-6 (P0/P1/P2) implémentés. P3 non traité (volontaire).
**Vérifications :** `tsc -b --noEmit` OK après chaque lot, `vite build` OK (lots 5-6),
`window.confirm` / `focus:outline-none` / `return null` page éliminés du périmètre,
`transition-all` éliminé, plus aucun `<a href>` / `window.location.href` interne.

- Lot 1 ✅ : `ConfirmDialog` partagé (`components/ui/confirm-dialog.tsx`), ChatInput labellisé,
  dropzone clavier, labels critiques, empties quiz/fiche/espace guidés (miroirs enseignant inclus).
- Lot 2 ✅ : autocomplete explicites, fix IME `JoinEspaceModal` (CSS + submit),
  `redirect` post-login consommé, `mot-de-passe-oublie` succès-only, erreur + Réessayer
  (`bienvenue`), radios/checkbox natifs (`DocRadio`, `DocCheckbox`), `radiogroup + label` partages.
- Lot 3 ✅ : `Progress` retransmet `value/max`, skeletons + `Réessayer` + `role=status/alert`
  (Chat, Documents, Étagère, dashboard), `ChatThread` vide guidé + `aria-busy`,
  `CodeBlock` try/catch + fallback, scroll `reduced-motion`.
- Lot 4 ✅ : `SpaceLayout` en `Link` SPA + header/nav accessibles, skip-link `#contenu`,
  sidebars suiveuses (tokens sémantiques, fini l'ardoise permanente), Chat 100 % sémantique,
  selects labellisés, hiérarchie h1/h2/h3, filtres étagère dans l'URL (`validateSearch`).
- Lot 5 ✅ : icônes `aria-hidden`, `transition-all` supprimés, rail FR décoratif,
  miroirs `hover → focus-visible`, `useClipboard` partagé + live, `useBlocker` quiz (x2),
  tables/mermaid/math navigables clavier.
- Lot 6 ✅ : référentiel badges unique (REJETÉE→« À revoir »/attention, quiz ≥80/≥50/erreur,
  abandon→secondary), `Bientôt disponible` retirés, responsive `p-4 sm:p-6 lg:p-8`,
  `tabular-nums`, `text-balance/pretty`, `card rounded-md`, CTA `flex-wrap`.
- Reporté (hors périmètre ou architectural) : `displayName` vs `userId.slice` (données API),
  agrégation `useConversations`/pagination >50, virtualisation, P3 (`index.html`, safe-area,
  `nuqs` étendu, dates `Intl` + `<time>`).
- UX : Lots 1,3,4,6 (destructifs, vides, navigation, copy).
- Accessibilité : Lots 1-5 (labels, live, focus, headings, scroll, icons).
- Responsive : Lot 6 + Lot 7 safe-area.
- Hiérarchie : Lot 4.
- Cohérence visuelle : Lots 4,6 (surfaces, badges).
- Typographie : Lot 6.
- Espacements : Lots 4,6 (`p-4 sm:p-6 lg:p-8`, CTA wrap).
- États : Lot 3.
- Interactions : Lots 1,2,5 (dropzone, radios, touch, guards, motion).
