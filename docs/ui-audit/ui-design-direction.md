# Direction visuelle — TsimokaAI

**Date :** 2026-09-27
**Statut :** direction à appliquer, sans reconstruction
**Sources :** `docs/ui-audit/ui-improvement-plan.md` + `frontend/src/styles/globals.css` + `frontend/index.html` + vérification `accueil.tsx`, `FicheCard.tsx`, `ChatMessage.tsx`, `card.tsx`, `button.tsx`, `badge.tsx` + skill `frontend-design`
**Périmètre :** frontend existant uniquement. Contrats API et logique métier inchangés. Aucun fichier `frontend/src` modifié par ce document.
**Articulation :** ce document fixe le cap esthétique. Le plan d'exécution reste `ui-improvement-plan.md` (Lots 1-7). Quand les deux se recouvrent, ce document tranche le « quoi viser », le plan tranche le « dans quel ordre réparer » (priorité Lots 4/6 pour la cohérence visuelle).

## 0. Principe directeur

> **Cahier d'étude, pas dashboard SaaS.**

TsimokaAI est un lieu où l'on lit, révise, discute d'un cours. L'interface doit évoquer le papier posé sur table et l'ardoise du dialogue, pas un outil de pilotage.

Règle de décision unique : **toute décision esthétique doit servir la lisibilité, la hiérarchie, l'identité Papier/Ardoise, ou l'orientation de l'utilisateur. Sinon, on la supprime.**

Ce qui existe déjà et qu'on garde comme identité :
- double surface **Papier (défaut) / Ardoise (chat uniquement)** ;
- trio typo verrouillé **Fraunces / IBM Plex Sans / IBM Plex Mono** ;
- rayon différencié **4px UI vs 2px Fiche** ;
- tags disciplinaires réservés au marquage disciplinaire.

Le travail à faire est donc du resserrage, pas du remplacement.

## 1. Direction visuelle générale

**Papier = conserver. Ardoise = dialoguer.**

- **Papier** (`frontend/src/styles/globals.css:18-24`) : tout sauf le chat. Fiches, étagère, tableaux de bord, paramètres, auth, landing. Fond chaud mat, cartes à peine plus claires, bordures visibles. Sens : stabilité, relecture, conservation.
- **Ardoise** (`globals.css:98-124`) : uniquement le wrapper de la route chat. Fond sombre vert-noir, texte craie. Sens : moment de discussion, concentration, éphémère. Le chat ne doit jamais ressembler à une page Papier assombrie.
- **Étanchéité stricte :** jamais les deux surfaces sur le même écran, sauf les îlots Papier déjà documentés (chips de citation, popovers — `globals.css:103`). Corriger les mélanges actuels (`ChatPage` + bandeau Papier + `bg-muted/secondary`, sidebar en `surface-ardoise` permanente hors chat — cf. Lot 4).
- **Sidebar suiveuse :** passer la sidebar en tokens sémantiques (`bg-sidebar`, `text-sidebar-foreground`) pour qu'elle suive la surface courante. Pas de sidebar Ardoise posée sur du Papier. Deux rendus assumés, zéro variante custom.
- **Aucun gradient décoratif, aucun fond illustré, aucune texture papier simulée.** La matérialité vient du couple fond/bordure + du rayon net de la Fiche, pas d'effets.

Wireframes d'intention :

```
Papier (étagère / fiche / dashboard)
+------------------------------------------------+
| header : h1 Fraunces + meta mono                |
|------------------------------------------------|
| [ tranche couleur ]  Titre fiche (Fraunces)     |
|                      Définition / Points / Ex.  |
|                      - - - - - - - - - - - - - |
|                      2 documents sources (mono) |
+------------------------------------------------+
| liste : une seule carte forte (la fiche),       |
| le reste en rangées sobres, pas en grille de    |
| clones                                           |
+------------------------------------------------+

Ardoise (chat uniquement)
+------------------------------------------------+
| ........... message assistant (texte nu) .......|
|                            [ bulle user raised ]|
| ........... message assistant (texte nu) .......|
|------------------------------------------------|
| [ input ardoise-raised : Écris ta question… ]   |
+------------------------------------------------+
```

Alignement : tout le contenu éditorial aligné à gauche. Seule la landing hero reste centrée (`max-w-2xl`, déjà en place dans `accueil.tsx:51`). Ne pas centrer des paragraphes de lecture ailleurs.

## 2. Hiérarchie

Objectif : en 3 secondes, on sait où lire en premier.

- **Pages :** un seul `h1` Fraunces par page. Sections en `h2` Plex Sans semibold ou Fraunces selon poids (hero et titres de fiches = Fraunces ; titres d'interface = Plex Sans). Cartes et questions quiz en `h3`. Ne jamais sauter de niveau (`h1→h3` actuel de `tableau-de-bord`, `accueil`, `connexion` à corriger — Lot 4).
- **Fiche (référence) :** ordre intangible, déjà bon dans `FicheCard.tsx:29-30` : `eyebrow mono FICHE > titre Fraunces xl > sections labellisées mono tag-sciences (Définition / Points clés / Exemple appliqué) > pied pointillé sources`. C'est la signature du produit : on ne la « modernise » pas.
- **Chat :** hiérarchie par retrait et air, pas par boîtes. Message assistant = texte courant nu (`text-base leading-relaxed`), message user = seule bulle (`ardoise-raised`, `rounded-md`, `border-border`). Citations en chips Papier compactes sous le message, jamais en cartes.
- **Landing :** hero (`h1` + sous-titre muted) → preuve (une vraie `FicheCard`, pas un mockup) → 3 piliers. Les 3 piliers actuels (`accueil.tsx:78-85`) sont trois cartes identiques `rounded-lg` : les passer en rangées à filets ou liste à puces fortes, avec titres différenciés. Pas de numérotation `01/02/03` : ce n'est pas une séquence, c'est un inventaire.
- **Dashboards / listes :** un chiffre-clé Fraunces par bloc max. Le reste en mono petit. Éviter la grille de 4 KPI identiques façon SaaS.

## 3. Typographie

Rôles verrouillés (`globals.css:126-131`, `index.html:14-16`). Jamais interchangés.

| Rôle | Famille | Usage | Interdit |
|------|---------|-------|----------|
| Display | **Fraunces** `font-display` | hero, titres de fiches, un chiffre-clé par page | UI courante, boutons, labels |
| Corps | **IBM Plex Sans** `font-sans` (défaut `body`) | tout le reste de l'UI | eyebrows, badges |
| Utilitaire | **IBM Plex Mono** `font-mono` | eyebrows, timestamps, `subject_tag`, `request_id`, badges, scores, pieds de fiche | phrases, paragraphes |

Échelle à appliquer :
- hero landing : `text-4xl font-semibold font-display` (existant, garder) ;
- titre fiche : `text-xl font-display font-semibold` (existant, garder) ;
- titre carte UI : `text-base font-display font-semibold` (`card.tsx:22`, garder) ;
- corps fiche/chat : `text-sm` à `text-base leading-relaxed` ;
- meta : `text-[0.65rem] font-mono uppercase tracking-wide text-muted-foreground`.

Règles :
- `text-balance` sur tous les headings, `text-pretty` sur paragraphes (Lot 6).
- Longueur de ligne `<80ch` en lecture (chat `max-w-2xl`, fiche pleine largeur mais paragraphes courts par construction).
- `tabular-nums` obligatoire sur scores, compteurs, `%`, dates (`font-mono` seul ne suffit pas — Lot 6).
- Placeholders en `ex : …` avec `…` (caractère unique, jamais `...`), `title` natif sur UUID proscrit → tooltip shadcn ou `[Source N]`.
- Copy : `tu` partout, voix active 2e personne, accents corrects, chaque erreur propose une action (« Vérifie le code auprès de ton enseignant », « Réessaie »). Dater ou retirer tout `Bientôt disponible`.

## 4. Système d'espacement

Rythme unique,_mobile-first :

- **Page :** `p-4 sm:p-6 lg:p-8` sur conteneurs, `px-4 sm:px-6 lg:px-8` sur headers/bandeaux (`SpaceLayout`, `tableau-de-bord`, `objectifs` — Lot 6). Chat resserré : `max-w-2xl px-4 sm:px-6`.
- **Cartes :** standard shadcn `p-5 gap-4` (`card.tsx:8`). Fiche : `p-5` + `gap-4` inter-sections + pied `mt-4 border-t border-dashed pt-3` (existant, garder comme marqueur).
- **Rythme vertical chat :** `mb-6` entre messages (existant `ChatMessage.tsx:51,62`), pas de bulles collées.
- **CTA :** toujours `flex flex-wrap gap-3`. Tester 360px : aucun bouton ne doit déborder ni exiger un scroll horizontal.
- **Densité :** ne pas compacter pour « faire tenir plus ». Si une liste dépasse 50 éléments, paginer (`Voir plus + total`) plutôt que densifier (Lot 6). Pastilles 16px autorisées seulement si le hit-target passe par le `label` parent (Lot 5).

## 5. Couleurs

Aucune couleur ajoutée sans passage par la page Notion « Contrat de Design » (`globals.css:8-11`). Palette de travail = tokens existants, en 5 pôles nommés :

1. **Papier** — `var(--papier-bg)` / `--papier-carte` / `--papier-border` : fond, carte, bordure. Texte `var(--encre)`, secondaire `var(--encre-muted)`.
2. **Ardoise** — `var(--ardoise-bg)` / `--ardoise-bg-raised` / `--ardoise-border` : fond chat, surélévation (bulle user, input, code), bordure. Texte `var(--craie)`, secondaire `var(--craie-muted)` (contraste à vérifier 4.5:1 — Lot 7).
3. **Encre/Craie** — jamais de noir pur (`#000`) ni de blanc pur (`#fff`) en aplats. Les primaires shadcn restent `encre` sur Papier et `craie` sur Ardoise (`--primary` scopé, garder).
4. **Tags disciplinaires** — `tag-sciences/info/lettres/eco/droit-shs/langues` : **réservés au marquage disciplinaire** (tranche `w-2` fiche, `w-1` chip, pastille). Jamais comme couleur de bouton, focus, illustration ou badge d'état. C'est ce qui les rend signifiants.
5. **États système** — `succes / attention / erreur` + référentiel badges unique (Lot 6) : `VALIDÉE→succes, EN_ATTENTE→secondary, REJETÉE→erreur, À revoir→attention`, quiz `≥80 succes / ≥50 attention / sinon erreur`, abandon `secondary` (jamais `erreur`), dates en texte mono (jamais en badge).

Usages tranchés :
- Focus/anneau : `var(--ring)` = `tag-sciences` + `:focus-visible 2px dashed var(--attention)` (`globals.css:220-223`, garder — c'est distinctif et visible).
- Liens : soulignés au survol, pas colorés en bleu générique.
- Erreurs courantes fiche : pastille `attention` + `⚠️ aria-hidden`, jamais seule couleur sans texte.

## 6. Formes et rayons

Le rayon est un signal hiérarchique, pas une décoration.

- **UI standard :** `--radius: 0.25rem (4px)` → `rounded-md` boutons, inputs, dialogs, popovers, bulles user (`button.tsx`, `ChatMessage.tsx:52`, garder).
- **Fiche et dérivés :** `--radius-fiche: 0.125rem (2px)` → `rounded-fiche` fiche, citation-chip, accordéons d'auto-quiz (`FicheCard.tsx:21,66,102`, garder). Effet carton net, volontairement plus raide que le SaaS arrondi.
- **Badges :** `rounded-full` + `font-mono uppercase` (`badge.tsx:8`, garder comme seul arrondi total autorisé).
- **Interdits :** `rounded-lg/xl/2xl` sur nouvelles cartes. Corriger les deux écarts existants : `card.tsx:8 rounded-lg` → `rounded-md`, `accueil.tsx:80 rounded-lg` (piliers) → filets ou `rounded-md`. Sceau gamification `rounded-full size-14` (`FicheCard.tsx:124`) : garder, c'est un insigne, pas une carte.

## 7. Usage des ombres

Sobriété quasi-totale. Le relief vient des bordures et des fonds, pas des ombres portées.

- **Autorisé :** `shadow-xs` boutons (`button.tsx:13-15`, garder), `shadow-sm` max sur fiche posée en preuve (landing `FicheCard ... shadow-md` actuel → descendre à `shadow-sm`).
- **Interdit :** `shadow-md/lg/xl`, `hover:shadow`, ombres colorées, double ombre bordure+ombre. Supprimer `hover:-translate-y-1` (`SpineCard`) et `hover:bg-papier-bg` sans équivalent focus (`QuizCard` — Lot 5).
- **Dialogs/sheets/popovers :** bordure `border-border` + fond `bg-card/popover`, sans sur-ombre. La hiérarchie vient du scrim, pas de l'ombre.

## 8. Composants à harmoniser

Ordre de convergence (sans changer l'API des composants) :

1. **Button / Input / Label** — base `h-9`, critique `min-h-11` mobile, `focus-visible:ring-2`, `outline-none` seulement avec remplacement (Lots 1/5). `autocomplete/name/inputmode` explicites par champ, pas de défaut magique dans `Input` (Lot 2).
2. **Badge** — seul référentiel d'états (§5). Uniformiser `rounded-full mono uppercase`, abandon en `secondary`.
3. **Card vs FicheCard** — ne jamais les confondre : `Card` = conteneur UI neutre (`rounded-md border`), `FicheCard` = objet éditorial (`rounded-fiche` + tranche tag + pied pointillé). Ne pas « fichiser » les cards UI ni « SaaSifier » la fiche en `rounded-lg shadow-md`.
4. **QuizCard / SpineCard / FicheCard (étagère)** — même grille, même pied meta mono, même `hover → focus-visible` miroir. `SpineCard` date `0.58rem→0.65rem min` (Lot 6).
5. **ChatInput / ChatMessage / CitationChips / CodeBlock / MessageRenderer** — 100% tokens Ardoise (`ardoise-raised/border`), chips citations en îlots Papier assumés, code/table/scroll avec `tabIndex=0 role=region aria-label` (Lots 4/5).
6. **Dialog / Sheet / Dropdown / Tabs / Accordion** — `overscroll-contain + max-h + overflow-y-auto`, transitions `height/opacity/transform` seules (pas `transition-all`), fermeture `Échap` + focus trap natif Radix (Lots 3/5).
7. **Progress / Skeleton** — `Progress` doit retransmettre `value/max` à `Root` (fix `aria-valuenow` quiz), skeletons aux bonnes dimensions pour éviter CLS + `aria-busy` (Lot 3).
8. **Empty / Error / Loading** — jamais `return null` (quiz `take.tsx:64`, `parametres.tsx:60`). Vide guidé + action (« Pose ta première question + exemples », « Aucune question — Retour/Régénérer »), erreur + `Réessayer (refetch)` (Lot 3).

## 9. États interactifs

- **Focus :** toujours visible. Garder `:focus-visible 2px dashed attention + outline-offset 2px` global + `focus-visible:ring-2` local sur contrôles. `focus-within:ring-2` sur dropzone. Résoudre le double `ring + dashed` par `outline-offset` ajusté, jamais en supprimant l'un sans remplacement (Lot 5).
- **Hover :** sobre (`bg-primary/90`, `bg-secondary/80`, `opacity-60→100` pastille persona). Chaque `hover:` sur carte doit avoir son miroir `focus-visible:` (`QuizCard`, `SpineCard`, `DocumentsPage` — Lot 5). `active:` légèrement plus contrasté que `hover:`.
- **Touch :** `min-h-11` contrôles critiques, `after:-inset-2` où la densité l'exige. Icônes `aria-hidden + focusable=false` systématique.
- **Formulaires :** labels visibles ou `sr-only` + `aria-invalid/describedby`, focus sur première erreur, erreurs typées avec next step (Lot 2). `JoinEspaceModal` : `text-transform:uppercase` CSS + `toUpperCase` au submit, jamais en `onChange` (casse IME).
- **Destructif :** `Dialog` de confirmation avec nom + conséquences + `Annuler` focus par défaut (pattern `fiches-a-valider:168-198`). Jamais `window.confirm` (Lot 1).
- **Saisie fragile :** `useBlocker` + `beforeunload` + `Dialog Quitter sans enregistrer ?` quand `dirty && !submitted` (quiz, persona draft — Lot 5).
- **Copie presse-papiers :** `try/catch + role=status + fallback sélection manuelle` (Lot 5).

## 10. Responsive

- **Paliers :** 360 / 768 / 1280 testés. Grilles avec paliers (`sm:`, `lg:`), pas de grille fixe 3 colonnes en mobile (corriger piliers landing).
- **Nav d'espace :** `overflow-x-auto whitespace-nowrap px-4 sm:px-6`, scroll clavier (`tabIndex=0 role=region aria-label="Onglets espace"`).
- **Tables / code / mermaid :** scroll clavier + `th scope=col`, `role=img aria-label + <details>Code source` pour mermaid/math (Lots 5/7).
- **Layout :** `min-h-svh` (pas `min-h-screen`), `pb-[env(safe-area-inset-bottom)]` sheets/drawers, `scroll-mt-16` ancres (Lot 7).
- **Media :** `figure` seulement si `url` truthy, `width/height` ou `aspect-ratio + loading=lazy decoding=async`, liens externes `target=_blank + ExternalLink aria-hidden + sr-only (nouvel onglet) + rel=noopener noreferrer` (Lot 7).

## 11. Animations

Principe skill `frontend-design` : un seul moment orchestré, le reste répond à l'action.

- **Autorisé :**
  - révélation craie du dernier message assistant (`use-chalk-reveal`, `ChatMessage.tsx:47`) — signature Ardoise, garder et throttler en `aria-live=polite` ;
  - `scrollIntoView` avec `behavior = reduced-motion ? auto : smooth` ;
  - accordéons/sidebar/sheet/dialog en `transform/opacity` 200ms max.
- **Interdit :** fade-slide-up par section au scroll, hover-transitions sur chaque carte, `transition-all`, parallaxe, loaders décoratifs. `autoFocus` desktop seul (`pointer:fine`), sinon focus sur erreur.
- **Motion réduite :** `prefers-reduced-motion` respecté partout (scroll, accordéons, chalk). Tester avec l'OS en réduit.

## 12. Anti-patterns explicitement refusés

| Pattern générique | Pourquoi refusé ici | Alternative TsimokaAI |
|---|---|---|
| Accumulation de cartes identiques | efface la hiérarchie lecture vs navigation | une Fiche forte + rangées sobres ; KPI : un Fraunces max par bloc |
| Gradients décoratifs gratuits | aucun sens sur papier/ardoise, bruit | aplat + bordure ; tranche tag comme seul accent |
| Surutilisation des ombres | contredit l'effet carton net 2px | §7 : `shadow-sm` max, relief par bordure |
| Effets sans fonction | distraient de la révision | une seule animation signature (craie), le reste fonctionnel |
| Esthétique SaaS générique (`rounded-2xl`, KPI-cards, eyebrow ALL-CAPS partout) | banalise un produit éducatif | eyebrows mono réservés aux meta, pas au marketing ; `rounded-md/fiche` stricts |
| Changements arbitraires « pour faire différent » | cassent la mémoire musculaire | toute dérive doit citer lisibilité, hiérarchie, identité ou UX — sinon rejetée |
| Numérotation `01/02/03` non séquentielle | encode une fausse séquence | pas de numéros sur les piliers (inventaire, pas processus) |
| `→` systématique, `A · B · C`, labels `WORD — fragment` | tics LLM/SaaS | verbes d'action français (« Créer », « Réessayer », « Voir plus »), phrases complètes |

## 13. Mise en œuvre sans reconstruction

- **Ne pas recréer :** tokens, trio typo, rayon fiche 2px, tranche tag, pied pointillé, `surface-ardoise` scopée, focus dashed. Ce sont l'identité.
- **Corriger en priorité (renvoi au plan) :** sidebar suiveuse + chat 100% Ardoise (Lot 4), `rounded-lg → rounded-md` (`card.tsx`, piliers landing) + `shadow-md → shadow-sm` (Lot 6), badges unifiés + `tabular-nums` (Lot 6), `hover → focus-visible` miroir + `transition-all` supprimés (Lot 5).
- **Vérifier avant de figer :** contraste `craie-muted/ardoise-bg` 4.5:1, snapshots 360/768/1280 après passage sidebar sémantique, rendu `Fraunces`/`Plex` self-hostés `@fontsource` (Lot 7, sortie du CDN Google Fonts).
- **Critère d'acceptation visuelle :** une capture Papier et une capture Ardoise doivent être immédiatement distinguables sans logo ; une Fiche doit être reconnaissable sans lire son titre (tranche + rayon 2px + pied pointillé).
