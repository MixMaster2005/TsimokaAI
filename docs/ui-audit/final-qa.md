# Audit QA final — Régression UI/UX du refacto frontend TsimokaAI

**Date** : 2026-09-27
**Périmètre** : refacto UI/UX non commité (65 fichiers modifiés + 2 nouveaux : `components/ui/confirm-dialog.tsx`, `hooks/use-clipboard.ts`), baseline `1d7072f` (« frontend baseline before UI audit »).
**Méthode** : lecture directe du code actuel + `git diff HEAD` fichier par fichier pour isoler l'apport du refacto. Référentiels chargés explicitement : `web-design-guidelines` (Web Interface Guidelines Vercel) et `vercel-react-best-practices`.
**Vérifications exécutées** : `npx tsc --noEmit` dans `frontend/` → **exit 0, 0 erreur**. Greps de cohérence (paddings racines, `confirmDelete`, `navigator.clipboard`, `aria-hidden`, headings, `key=`, `isLoading` manquants).
**Fichiers sources non modifiés** : audit en lecture seule, seul ce rapport est écrit.

## Verdict

**Aucune régression critique (P0).** Le refacto est globalement un progrès net : labels `sr-only`, `role="alert"`/`role="status"`, `aria-pressed`, empty/error/loading avec retry, `DocCheckbox`/`DocRadio` natifs, garde anti-perte `useBlocker`, filtres étagère dans l'URL (zod), skip-link, `tabular-nums`, responsive `p-4 sm:p-6`.

Il reste **9 problèmes importants (P1)** à corriger avant de considérer le refacto comme terminé — dont 1 bug fonctionnel avéré (décalage d'index quiz), 1 faille de sécurité (open redirect), 1 régression de contrat anti-énumération, et des états d'erreur silencieux sur des actions critiques (verdict enseignant, annotations, suppressions). Le reste (P2/P3) peut être planifié.

**Légende** : P0 = régression critique (bloquant) · P1 = problème important (corriger maintenant) · P2 = amélioration utile (planifier) · P3 = détail mineur / dette.

---

## P0 — Régressions critiques

**Aucune.** Rien de bloquant pour l'usage normal : aucune page blanche systématique, aucune perte de données systématique, `tsc` vert.

---

## P1 — Problèmes importants (corriger maintenant)

### P1-1 — Décalage d'index QCM/QRC entre passage du quiz et résultats
- **Fichier/composant** : `frontend/src/routes/_app/espaces/$spaceId/quiz/$quizId/take.tsx:32-35` + `frontend/src/features/quiz/components/QuizResultsPanel.tsx:15,32` (miroir enseignant identique).
- **Problème** : le passage parse avec `parseQuizQuestions` (`filterQRC=true`), les résultats relisent avec `parseAllQuizQuestions` (`filterQRC=false`) — vérifié dans `parse-quiz-content.ts:29-37,44-45`. Les `questionIndex` soumis (base filtrée) sont relus dans le tableau non filtré.
- **Raison** : deux parsers divergents sur le même `contentJson`, index positionnel sans mapping par ID.
- **Impact** : dès qu'une question QRC (sans options) précède un QCM, le feedback « Bonne réponse » pointe la mauvaise question. Bug fonctionnel silencieux sur l'écran cœur pédagogique.
- **Correction recommandée** : un seul parser des deux côtés (filtrer partout ou nulle part) ou mapper les réponses sur un identifiant stable plutôt que sur l'index.

### P1-2 — Open redirect via `redirect=//evil.com`
- **Fichier/composant** : `frontend/src/features/auth/components/LoginForm.tsx:13` + `frontend/src/features/auth/api/use-login.ts:25`.
- **Problème** : garde `startsWith('/')` accepte `//evil.com`, puis `navigate({ to })` suit la cible.
- **Raison** : validation de préfixe insuffisante sur un paramètre d'URL attaquable (`/connexion?redirect=…`).
- **Impact** : phishing (redirection post-login vers un domaine hostile). Sécurité, pas seulement UX.
- **Correction recommandée** : `startsWith('/') && !startsWith('//')`.

### P1-3 — Mot de passe oublié : régression du contrat anti-énumération
- **Fichier/composant** : `frontend/src/routes/_public/mot-de-passe-oublie.tsx:21-28`.
- **Problème** : le refacto passe de `onSettled: () => setEnvoye(true)` à `onSuccess` uniquement ; en erreur, le formulaire reste affiché avec `forgotPassword.error.message`.
- **Raison** : changement de callback mutation qui modifie le contrat observable.
- **Impact** : fuite d'existence de compte + incohérence avec le message « Si un compte existe… » affiché en succès.
- **Correction recommandée** : revenir à `onSettled: () => setEnvoye(true)`, ou afficher un message générique strictement identique en succès et en erreur.

### P1-4 — Flash « introuvable » pendant le chargement (fiches, quiz, paramètres espace)
- **Fichier/composant** : `routes/_app/espaces/$spaceId/fiches/$ficheId.tsx:34`, `routes/_app/.../quiz/$quizId/index.tsx:22`, `routes/_app/.../parametres.tsx` espace `:59`, `take.tsx:80` — + 3 miroirs enseignant.
- **Problème** : `if (!fiche)` / `if (!quiz)` / `if (!space)` sans tester `isLoading`. `useFiche`/`useQuiz`/`useEspace` retournent `undefined` pendant le fetch (vérifié : aucun `isLoading` dans ces branches).
- **Raison** : les nouveaux écrans « introuvable » (par ailleurs bienvenus) ne distinguent pas chargement et 404 ; le `loader ensureQueryData` ne garantit rien en navigation directe/refresh lent.
- **Impact** : faux 404 affiché à tort puis remplacé — régression UX visible à chaque chargement lent ; côté paramètres espace, `useInviteCode(spaceId, true)` fetche en plus inutilement sur 404.
- **Correction recommandée** : `if (isLoading) return <skeleton/null> ; if (!data) return <not-found>` sur les 7 sites.

### P1-5 — Verdict enseignant en échec silencieux
- **Fichier/composant** : `frontend/src/features/fiches/components/ValidationSection.tsx:37-48,82-121`.
- **Problème** : seul `validateFiche.isPending` est consommé ; `validateFiche.isError` n'est jamais rendu (vérifié : aucun bloc d'erreur dans le fichier, contrairement aux modales du refacto qui ajoutent toutes `<p role="alert">`).
- **Raison** : oubli dans la systématisation des états d'erreur.
- **Impact** : l'enseignant clique « Valider / À revoir », l'appel échoue, rien ne s'affiche — action critique sans feedback.
- **Correction recommandée** : ajouter le même bloc `role="alert"` que `PersonaModal`, avec consigne (« corrige puis réessaie »).

### P1-6 — Annotations : chargement et erreur d'envoi silencieux
- **Fichier/composant** : `frontend/src/features/fiches/components/AnnotationsSection.tsx:16,42-47,71`.
- **Problème** : `useQuery` sans `isLoading`/`isError` (vérifié) ; `addAnnotation.isError` jamais rendu ; quand `annotations === undefined`, ni liste ni empty ni skeleton.
- **Raison** : section non couverte par la passe loading/error du refacto.
- **Impact** : section blanche confondue avec « zéro annotation » ; annotation perdue sans message en cas d'échec réseau.
- **Correction recommandée** : skeleton + `<p role="alert">` sur `addAnnotation.error`, en réutilisant les patterns de `DocumentsPage`.

### P1-7 — Suppressions compte/espace sans vraie modale (4 sites contournent `ConfirmDialog`)
- **Fichier/composant** : `routes/_app/parametres.tsx:25,157-160`, `routes/_app/espaces/$spaceId/parametres.tsx:47,166-169`, + 2 miroirs enseignant (vérifié par grep : 4× `confirmDelete`, zéro `window.confirm` restant).
- **Problème** : swap inline « Confirmer la suppression / Annuler » au lieu du nouveau `<ConfirmDialog>` pourtant adopté par `DocumentsPage` et `MembresPage*`.
- **Raison** : migration partielle : le composant partagé existe (`confirm-dialog.tsx:29`) mais les 4 sites destructifs n'ont pas été migrés.
- **Impact** : suppression de compte/espace sans focus-trap, sans `DialogDescription` des conséquences, sans état `isPending` (double-clic = double `mutate`) — incohérence avec le standard que le refacto lui-même établit.
- **Correction recommandée** : migrer les 4 sites vers `<ConfirmDialog open onOpenChange title description confirmLabel onConfirm isPending>`.

### P1-8 — Chat : `<main>` imbriqué, skip-link inefficace sur l'écran cœur
- **Fichier/composant** : `frontend/src/components/shared/ChatPage.tsx:152-153` + `routes/_app/route.tsx:52` (+ `routes/enseignant/route.tsx`).
- **Problème** : `ChatPage` monte un second `SidebarProvider` + `SidebarInset` (qui rend `<main>`) à l'intérieur du `SidebarInset` (`<main id="contenu">`) du layout (vérifié en lecture).
- **Raison** : l'imbrication pré-existait, mais le refacto l'a rendue fonctionnellement problématique en ajoutant le skip-link + `id="contenu"` sur le wrapper externe sans traiter l'interne.
- **Impact** : landmarks dupliqués/confus pour les lecteurs d'écran sur l'écran le plus utilisé ; « Aller au contenu » n'amène pas au chat.
- **Correction recommandée** : remplacer le `SidebarInset` interne de `ChatPage` par une `<div>` aux mêmes classes ; garder un seul `<main id="contenu">`.

### P1-9 — Paramètres : `displayName` vide soumettable (divergence avec le miroir enseignant)
- **Fichier/composant** : `frontend/src/routes/_app/parametres.tsx:33-36`.
- **Problème** : `updateProfile.mutate({ displayName })` sans `trim`/`required`/garde, alors que `enseignant/parametres.tsx:33-37` a `if (!trim) return` + `required` + `trim()` (vérifié). `useState(user?.displayName ?? '')` reste `''` si la session est lente.
- **Raison** : divergence des deux copies quasi identiques (~95 %).
- **Impact** : envoi d'un nom vide → erreur back ou profil effacé.
- **Correction recommandée** : aligner sur la version enseignant, puis factoriser (voir P2-11).

---

## P2 — Améliorations utiles (planifier)

### P2-1 — `ABANDONNE` visuellement identique à `EN_COURS`
- **Fichier** : `routes/_app/objectifs.tsx:31-35` (vérifié ; le diff montre `ABANDONNE: 'erreur'` → `'secondary'`).
- **Problème** : `EN_COURS` et `ABANDONNE` partagent le variant `secondary`.
- **Raison** : changement du refacto, probablement pour adoucir le rouge — mais deux statuts distincts deviennent indistinguables.
- **Impact** : perte d'information statutaire (pas seulement un choix de style).
- **Correction** : variant distinct pour `ABANDONNE` (ex. `outline` ou teinte neutre dédiée), en gardant l'absence de rouge si c'est la direction voulue.

### P2-2 — `updateEspace` sans feedback erreur/succès
- **Fichier** : `routes/_app/espaces/$spaceId/parametres.tsx` (§ formulaire, ~l.80-100) + miroir enseignant.
- **Problème** : aucun `isError`/`isSuccess` rendu pour « Enregistrer » (ni pour la suppression).
- **Raison** : la passe `role="alert"` du refacto n'a pas couvert ce formulaire.
- **Impact** : échec de sauvegarde silencieux, l'utilisateur croit ses modifications enregistrées.
- **Correction** : `<p role="alert">` d'erreur + confirmation de succès comme sur les formulaires auth.

### P2-3 — États « vide » trompeurs : étagère filtrée + tableau de bord sans CTA
- **Fichier** : `features/espaces/components/EtagereGrid.tsx:57-96` (vérifié) ; `routes/_app/tableau-de-bord.tsx:54-60` (vu en diff).
- **Problème** : si `espaces.length > 0` mais `espacesFiltres.length === 0`, les deux `EtagereSection` affichent leur `emptyMessage` (« crée le premier… », « rejoins-en un… »). Côté tableau de bord, « Crée ou rejoins un espace » n'a aucun lien/bouton.
- **Raison** : aucun état « 0 résultat pour ces filtres » ; empty state textuel sans issue.
- **Impact** : l'utilisateur croit n'avoir aucun espace alors que c'est le filtre ; cul-de-sac UX côté dashboard.
- **Correction** : bloc « Aucun résultat » + bouton reset vers `FILTRES_ETAGERE_DEFAUT` quand total > 0 et filtré vide ; CTA vers `/` ou `JoinEspaceModal` côté dashboard.

### P2-4 — `RadioGroup` des réponses sans nom accessible
- **Fichier** : `features/quiz/components/QuizQuestionCard.tsx:27` (vérifié).
- **Problème** : `<RadioGroup value onValueChange>` sans `aria-label`/`aria-labelledby` ; le `<h3>` question ne labellise pas le groupe Radix.
- **Raison** : oubli dans la passe a11y (les compteurs quiz ont reçu `aria-pressed`, pas ce groupe).
- **Impact** : lecteur d'écran annonce un groupe de radios sans nom.
- **Correction** : `aria-labelledby` vers l'id de la question (ou `aria-label="Réponses question N"`).

### P2-5 — Groupes radio des modales de génération sans `fieldset`/`legend`
- **Fichier** : `features/fiches/components/GenerateFicheModal.tsx:91,125` (vérifié : `<Label>Stratégie</Label>` / `<Label>Périmètre</Label>` orphelins + radios natifs `name="strategy"`/`name="perimetre"` sans groupe nommé) ; `GenerateQuizModal.tsx` (périmètre/difficulté — seul le compteur a reçu `role="group"` + `aria-pressed`, vérifié `:240-246`).
- **Problème** : les modales Share ont été corrigées (`radiogroup` + `aria-label`), pas celles-ci.
- **Raison** : systématisation incomplète.
- **Impact** : le lecteur d'écran perd le nom du groupe (« Stratégie / Périmètre / Difficulté »).
- **Correction** : wrapper `role="radiogroup" aria-label="…"` ou `fieldset`/`legend` natif.

### P2-6 — Dropzone documents : double arrêt Tab (`role="button"` + `<input>` focusable)
- **Fichier** : `components/shared/DocumentsPage.tsx:84-125` (vérifié).
- **Problème** : la `div role="button" tabIndex={0}` enveloppe un `<input type="file" class="sr-only">` qui reste dans l'ordre de tabulation → 2 arrêts pour la même action + interactif imbriqué dans `role=button`.
- **Raison** : ajout clavier louable mais pattern `div-bouton + input` non assaini.
- **Impact** : confusion SR (« bouton » contenant « choisir des fichiers »), Tab redondant.
- **Correction** : `tabIndex={-1}` + `aria-hidden` sur l'input (la div porte déjà `aria-label`/`aria-describedby`), ou basculer sur un vrai `<label>`.

### P2-7 — `SidebarRail` : `aria-hidden` contradictoire avec `aria-label`
- **Fichier** : `components/ui/sidebar.tsx:282-305` (vérifié par grep : `aria-label="Toggle Sidebar"` + `aria-hidden="true"` + `tabIndex={-1}` sur le même `<button>`).
- **Problème** : `aria-hidden` masque le contrôle aux technologies d'assistance alors qu'il garde label, `title` et `onClick` souris.
- **Raison** : ajout du refacto non réconcilié avec le bouton existant.
- **Impact** : contrôle fantôme pour SR/clavier (seul `Ctrl+B` reste, non découvrable).
- **Correction** : supprimer `aria-hidden` (garder `tabIndex={-1}`) ; passer `title`/`aria-label` en français pour la cohérence.

### P2-8 — `ConfirmDialog` : libellé pending hardcodé « Suppression… »
- **Fichier** : `components/ui/confirm-dialog.tsx:50-52` (vérifié) ; usages `MembresPage.tsx:63`, `MembresPageEnseignant.tsx:67` (`confirmLabel="Quitter"`), `:317/:337` (`"Retirer"`).
- **Problème** : `{isPending ? 'Suppression…' : confirmLabel}` quel que soit le `confirmLabel`.
- **Raison** : nouveau composant partagé, libellé non paramétré.
- **Impact** : l'utilisateur qui quitte un espace lit « Suppression… » — incohérence anxiogène sur action destructive.
- **Correction** : prop `pendingLabel?` ou fallback `` `${confirmLabel}…` ``.

### P2-9 — `MembresPage*` : aucun état loading/error sur invitation, groupes, membres
- **Fichier** : `components/shared/MembresPage.tsx` (+ miroir `MembresPageEnseignant.tsx`) — vérifié : aucun `isLoading`/`isError`/`Skeleton`/`refetch` dans le fichier hors live region clipboard (`:253`).
- **Problème** : `useInviteCode`/`useGroupes`/`useMembres` consommés nus (`if (!inviteCode) return null`, `membres?.map` sur `undefined` → liste vide silencieuse).
- **Raison** : le refacto a systématisé loading/error (`ChatPage`, `ChatThread`, `DocumentsPage`) mais pas ici.
- **Impact** : blanc sans feedback au chargement ; erreur réseau silencieuse pour le propriétaire.
- **Correction** : mêmes patterns que `DocumentsPage` (`Skeleton` + `role="status"`, bloc erreur + `refetch`).

### P2-10 — `CodeBlock` duplique `useClipboard` (timer fuyant)
- **Fichier** : `features/chat/components/CodeBlock.tsx:9-18` vs `hooks/use-clipboard.ts:8-36` (vérifié : seul consommateur restant de l'ancien pattern par grep).
- **Problème** : `copied`/`setTimeout(2000)` inline sans cleanup au démontage → `setState` sur composant démonté ; message live moins riche que le hook.
- **Raison** : double implémentation du même pattern ; le hook (avec `clearTimeout` au unmount + état `echec` textuel) existe mais n'est pas utilisé ici.
- **Impact** : warning React potentiel + feedback moins précis.
- **Correction** : utiliser `useClipboard()`, garder uniquement le repli « sélection manuelle » (`:22-28`) en complément.

### P2-11 — Racines de pages en `p-8` fixe + ligne d'actions enseignant sans `flex-wrap`
- **Fichier** : `routes/_app/mes-fiches.tsx:24` (`p-8`), `routes/enseignant/index.tsx:41` (`p-8 pb-0` + états `px-8 py-6`), `routes/enseignant/tableau-de-bord.tsx:26` (`p-8`) — vérifié par grep ; `routes/enseignant/espaces/$spaceId/quiz/$quizId/index.tsx:93` (`flex gap-2` sans wrap, vérifié) alors que la vue étudiante a reçu `flex-wrap`.
- **Problème** : standard migré `p-4 sm:p-6 lg:p-8` non appliqué à ces racines (les `p-6` de `quiz/index`, `fiches/index`, `dashboard` enseignant sont déjà conformes — vérifié, à ne pas toucher — tout comme le `p-8` de la zone de drop `DocumentsPage.tsx:99`, volontaire).
- **Raison** : migration responsive partielle.
- **Impact** : gouttières 32 px forcées sur mobile 360 px ; débordement horizontal possible sur la ligne d'actions enseignant.
- **Correction** : aligner chaque racine listée + ajouter `flex-wrap` côté enseignant.

### P2-12 — Accessibilité formulaires restante (labels orphelins, `h1` manquant, erreurs sans `role`)
- **Fichier** : `routes/_public/mot-de-passe-oublie.tsx:34,50` (deux `h2` top-level, zéro `h1` — vérifié — alors que `connexion`/`inscription` sont passés en `h1`) ; `routes/_app/parametres.tsx:90-92` + miroir enseignant (`<Label>Email</Label>` sans `htmlFor`, `<Input disabled>` sans `id` — vérifié) ; `routes/_app/espaces/$spaceId/parametres.tsx:95` + miroir (`<Label>` « Persona pédagogique » sans contrôle associé) ; `routes/_app/objectifs.tsx:154-156,244-253` (inputs date/message avec seul `title`/`placeholder` — vérifié) ; `routes/enseignant/fiches-a-valider.tsx:177-184,251` (`textarea` de rejet sans label, `{erreur}` sans `role="alert"` — vérifié `:184` `text-red-600` brut — et recherche `placeholder` seul) ; erreur persona sans `role="alert"` (`parametres.tsx` espace `:118-120` + miroir).
- **Problème** : résidus non couverts par la passe `sr-only`/`role="alert"` du refacto.
- **Raison** : systématisation incomplète fichier par fichier.
- **Impact** : rattachement SR faible, erreurs non annoncées, outline a11y incohérente.
- **Correction** : `label sr-only` + `id`, `htmlFor`/`id` ou `aria-label`, `h1` sur mot-de-passe-oublié, `role="alert"` manquants, remplacer le `<Label>` section par un heading.

### P2-13 — Échelle Badge score divergente étudiant/enseignant
- **Fichier** : `routes/_app/.../quiz/$quizId/index.tsx:93-100` (`>=50→attention, <50→erreur`, vérifié en diff) vs `routes/enseignant/.../quiz/$quizId/index.tsx:128-135` (`>=50→default, <50→attention`, vérifié).
- **Problème** : même 45 % = rouge côté élève, ambre côté prof ; 65 % = ambre vs primary. Variantes toutes définies (pas de crash).
- **Raison** : les deux miroirs ont évolué séparément pendant le refacto.
- **Impact** : incohérence de lecture du score selon le rôle.
- **Correction** : une échelle unique partagée (constante ou mini-composant).

### P2-14 — N+1 requêtes sur l'étagère (latent, pré-existant — à cadrer, pas à imputer au refacto)
- **Fichier** : `routes/_app/index.tsx:70-75` (1× `conversationsQueryOptions(space.id)` par espace pour n'afficher que `latestConv`) ; même fan-out `use-espaces-stats.ts:23-37`, `use-all-fiches.ts`.
- **Problème** : page d'accueil en O(N) requêtes ; chaque requête rejoue le refresh 401 (`api-client.ts:123-130`).
- **Raison** : pas d'endpoint agrégé backend ; pattern antérieur au refacto, non aggravé par lui.
- **Impact** : dégradation linéaire avec le nombre d'espaces ; acceptable à N petit.
- **Correction** : endpoint agrégé (`/spaces/stats`, `/conversations/latest`) ou restreindre le fan-out (seulement si la section « Reprendre » est visible). Garder `staleTime 30s` / `retry:1` / `refetchOnWindowFocus:false` (`query-client.ts:18-20`).

---

## P3 — Détails mineurs / dette

### P3-1 — Hiérarchie headings quiz : `h2` nominal vs `h1` d'erreur, résultats en `h2` sans `h1`
- **Fichier** : `routes/_app/.../quiz/$quizId/index.tsx:45` (`h2` titre nominal) vs `:26` (`h1` « Quiz introuvable », vérifié) ; `take.tsx:104` (« Résultats » en `h2`) + compteur en `span` (`take.tsx:139-142`) sans aucun `h1` dans le flux.
- **Correction** : titre détail en `h1`, promouvoir le compteur take ou ajouter un `h1 sr-only`.

### P3-2 — Copy « Retour aux fiches » depuis un quiz
- **Fichier** : `routes/_app/.../quiz/$quizId/index.tsx:28-32` (+ miroir enseignant, vérifié en diff : « Retourne à la liste des fiches » + lien `/espaces/$spaceId/fiches` depuis une page quiz) ; `take.tsx:86,93` (« Retourne à la fiche » au singulier + primaire « Retour aux fiches » au lieu de « Retour au quiz »).
- **Correction** : pointer vers l'espace/quiz + copy « Retour à l'espace / au quiz ».

### P3-3 — `EtagereGrid` contrôlé/incontrôlé fragile + recherche sans debounce
- **Fichier** : `EtagereGrid.tsx:26-29` (`value = filtres ?? local`, `setValue = onFiltresChange ?? setLocal` — `filtres` seul sans handler = UI figée ; latent car les deux sont passés aujourd'hui) ; `routes/_app/index.tsx:58-68` (chaque frappe → `navigate(replace)`, vu en diff).
- **Correction** : guard + debounce de la recherche.

### P3-4 — Clés React sur contenu textuel
- **Fichier** : `features/fiches/components/FicheCard.tsx:40,52` (`key={point}` / `key={mistake}` — vérifié ; `self_quiz` utilise déjà l'index `:65`).
- **Problème** : deux `key_points` identiques → warning doublon + mauvais diff.
- **Correction** : `` key={`${point}-${i}`} ``.

### P3-5 — `QuizTake` étudiant/enseignant dupliqué (~180 lignes)
- **Fichier** : `routes/_app/.../quiz/$quizId/take.tsx` vs `routes/enseignant/.../quiz/$quizId/take.tsx` (seules la bannière préviz + copy Dialog changent ; `useBlocker` présent des deux côtés — vérifié).
- **Impact** : tout fix anti-perte/empty (P1-1, P1-4) doit être porté deux fois.
- **Correction** : page partagée `QuizTakeView` + prop `previewBanner`.

### P3-6 — États `ChatPage` en `p-6` fixe + incohérence `surface-ardoise`
- **Fichier** : `ChatPage.tsx:55,74,126` (vérifié) vs branches nominales en `px-4 sm:px-6` (`:93,155,160`).
- **Correction** : `p-4 sm:p-6` sur les 3 états + vérifier l'ardoise sur la branche vide.

### P3-7 — `MathBlock` scrollable non focusable au clavier
- **Fichier** : `features/chat/components/MathBlock.tsx:43` (`overflow-x-auto` sans `tabIndex` — vérifié) alors que `pre` code, tables et mermaid ont reçu `tabIndex={0}` + `role="region"` + label (`MermaidBlock.tsx:44,52-53`, vérifié).
- **Correction** : même pattern (`tabIndex={0} role="region" aria-label="Formule…"`).

### P3-8 — `ConfirmDialog` fermable (Escape/overlay/Annuler) pendant `isPending`
- **Fichier** : `confirm-dialog.tsx:40,47` (vérifié : `onOpenChange` et Annuler toujours actifs).
- **Impact** : faible (la mutation continue), mais perte du retour visuel de l'action destructive.
- **Correction** : quand `isPending`, ignorer `onOpenChange(false)` et `disabled` sur Annuler.

### P3-9 — Bruit et micro-incohérences
- `LoginForm.tsx:27` : `noValidate={false}` explicite le défaut — supprimer.
- Écrans « introuvable » ×7 sans composant partagé (`main.tsx:22` + 6 écrans fiches/quiz/espace ×2 rôles) : extraire `EntityNotFound({eyebrow, titre, texte, lien})`, passer `main.tsx:22` en `p-4 sm:p-6`.
- `routes/_app/tableau-de-bord.tsx` : bloc « Taux par matière » muet si vide/loading (header seul) → texte « Aucune donnée par matière pour l'instant. ».
- `onboarding/creer-espace.tsx:68` : erreur sans focus ni `aria-invalid` (vs `erreurRef.focus()` de Login/Register).
- Échec clipboard paramètres espace visible uniquement en `sr-only` (bouton restant « Copier ») : ajouter un message visible + `role="status"`.
- `katex.min.css` global (`globals.css:3`) alors que `katex`/`mermaid` JS sont en `await import()` (`MathBlock.tsx:17`, `MermaidBlock.tsx:16` — pattern à garder) : ne rien changer sans mesure ; import CSS conditionnel éventuel plus tard.
- Pas de virtualisation (`.map` simples, aucune dep `react-window/virtuoso`) : acceptable sans preuve de >50 items — signal latent uniquement.

---

## 1. Corrections nécessaires maintenant (P1)

| # | Action | Fichiers |
|---|--------|----------|
| 1 | Unifier le parser quiz passage/résultats (ou mapper par ID) | `take.tsx` ×2 rôles, `QuizResultsPanel.tsx` |
| 2 | Bloquer l'open redirect `//` | `LoginForm.tsx:13`, `use-login.ts:25` |
| 3 | Restaurer `onSettled` (ou message identique succès/erreur) sur mot de passe oublié | `mot-de-passe-oublie.tsx:21-28` |
| 4 | Brancher `isLoading` avant chaque écran « introuvable » / « aucune question » | 7 sites fiches/quiz/paramètres + `take.tsx` ×2 |
| 5 | Afficher `role="alert"` sur échec de verdict + échec d'annotation (+ skeleton/empty annotations) | `ValidationSection.tsx`, `AnnotationsSection.tsx` |
| 6 | Migrer les 4 suppressions compte/espace vers `ConfirmDialog` (avec `isPending`) | `parametres.tsx` ×2 rôles, `espaces/$spaceId/parametres.tsx` ×2 rôles |
| 7 | Supprimer le `<main>` imbriqué du chat (remplacer `SidebarInset` interne par `<div>`) | `ChatPage.tsx:152-153` |
| 8 | Garder `displayName` non vide (aligner sur le miroir enseignant) | `routes/_app/parametres.tsx:33-36` |

## 2. Améliorations pouvant attendre (P2 → P3, ordre suggéré)

1. Distinguer `ABANDONNE` de `EN_COURS` (P2-1) + unifier l'échelle Badge score inter-rôles (P2-13).
2. Feedback `updateEspace` + états loading/error `MembresPage*` (P2-2, P2-9).
3. Empty « 0 résultat de filtre » étagère + CTA dashboard (P2-3).
4. Noms de groupes radio : `QuizQuestionCard`, modales Generate (P2-4, P2-5) + résidus labels/roles P2-12.
5. Dropzone double Tab + `SidebarRail` `aria-hidden` + `pendingLabel` (P2-6, P2-7, P2-8).
6. `CodeBlock` → `useClipboard` ; factoriser `QuizTake`, écrans `introuvable`, formulaires paramètres (P2-10, P3-4/5/9).
7. Responsive restant (`p-8` racines, `flex-wrap` enseignant, états `ChatPage`) + headings quiz + copies « Retour… » (P2-11, P3-1/2/6).
8. Dette : `key` FicheCard, debounce recherche étagère, `MathBlock` focus, `ConfirmDialog` pendant pending, `noValidate`, N+1 agrégé, CSS KaTeX (P3-3/4/7/8/9, P2-14).

## 3. Éléments à préserver (ne pas régresser)

1. **`window.confirm` → `ConfirmDialog`** (`DocumentsPage`, `MembresPage*`) : Annuler en `autoFocus`, conséquences nommées — étendre, ne pas revenir en arrière.
2. **États loading/error/empty + retry** (`ChatPage`, `ChatThread`, `DocumentsPage`, `SpaceLayout`, étagère, tableau de bord) avec `role="status"`/`role="alert"` + `aria-busy` + textes `sr-only` : zéro état blanc.
3. **`use-clipboard.ts` + live regions** : remplace les `writeText` nus qui throwaient sans feedback — généraliser (cf. P2-10).
4. **`DocCheckbox`/`DocRadio` en `<label>` natif** (`GenerateFicheModal`, `GenerateQuizModal`) : `min-h-11`, `has-checked:`, input natif — ne pas revenir au pattern `button > Checkbox`.
5. **Garde anti-perte `useBlocker` + `Dialog`** (`take.tsx` ×2 rôles, SPA + `beforeunload`, `reset` sur « Rester », pas de bloc après `submitted`).
6. **Filtres étagère dans l'URL (zod)** (`_app/index.tsx`) : clés courtes, valeurs non-défaut seules, fallback défaut sur invalide, `replace:true` — partageables, jamais d'écran cassé. Étendre à la vue enseignant restée en état local.
7. **Contrat Markdown sûr** (`MessageRenderer` : `h1→h2`, `loading="lazy"`/`decoding="async"`, garde `if (!block.url) return null`) + **libs lourdes en `await import()`** (`MathBlock`, `MermaidBlock`).
8. **Fondations** : skip-link + `id="contenu"` + `focus-visible`, régions scrollables clavier, `selects` labellisés `h-11`, `aria-hidden` sur icônes décoratives, `tabular-nums` sur chiffres/dates, `text-balance`/`text-pretty` headings, `prefers-reduced-motion` (`ChatThread`), contrat surfaces Papier/Ardoise (`globals.css`), `query-client` (`staleTime 30s`, `retry:1`, `refetchOnWindowFocus:false`) + `loader ensureQueryData` (pas de `fetch()` brut hors `api-client`).
9. **Tampon validation jamais rouge** (`ValidationSection`, `VALIDÉE/À REVOIR` en `--succes`/`--attention`) — cohérence avec la correction P2-1.

---

## Annexe — méthode et traçabilité

- Chaque finding ci-dessus a été vérifié en lecture directe du fichier cité (ligne approximative = localisation du code fautif, pas du diff).
- `git diff HEAD` n'a servi qu'à distinguer l'apport du refacto du pré-existant ; les points notés « pré-existant » (P2-14, partie de P1-8) le sont explicitement et ne sont pas imputés au refacto.
- Aucun choix de style n'est contesté : les findings portent sur des comportements observables (bug, sécurité, faux 404, silence d'erreur, landmarks, tabulation, information perdue).
- `tsc --noEmit` : 0 erreur — aucun finding TypeScript bloquant ; les points React/perf signalés sont des observations de code, pas des hypothèses d'exécution.
