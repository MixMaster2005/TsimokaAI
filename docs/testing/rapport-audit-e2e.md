# Rapport d'audit E2E — TsimokaAI (15/10/2026)

Audit fonctionnel complet exécuté en navigateur réel (MCP Playwright) + API, avec
corrections itératives et re-tests. Périmètre : toute l'application **sauf le chat**,
gelé par consigne (seul le **persona** reste testable/corrigeable).

- Exécution : 15/10/2026, stack Docker complète reconstruite (`--profile ollama`).
- Provider LLM actif : **Groq** (`ACTIVE_LLM_PROVIDER=groq`) ; embeddings Ollama
  (`nomic-embed-text`) ; vision Gemini disponible mais non sollicitée (PDF texte).
- Comptes de test (données fictives, préfixe `qa-`, isolées des données existantes) :
  - enseignant `qa-ens-20261015@test.local` / espace `Physique QA — Mécanique (modifié)`
    (`19ce33f4-5f8f-40bb-b75f-9f27d0eff498`, code `GZ35W7S6`) ;
  - étudiant membre `qa-etu-20261015@test.local` (Lova, invitée par code) ;
  - étudiante créatrice `qa-etu2-20261015@test.local` (Nirina, espace
    `Chimie QA — Atomes` `89d58cad-0e80-4af8-85a7-ecd30c8a443e`).
- Supports de test : `cours-mecanique-qa.pdf` (3 chunks) et `cours-chimie-qa.pdf`
  (2 chunks), générés pour l'audit (conservés dans `.artifacts/e2e/`, non versionnés).
- Mot de passe de test unique utilisé pour les 3 comptes (non reproduit ici, non commité).

## A. Résumé

- État général : **fonctionnel de bout en bout après 4 corrections**. Les 3 workflows
  (enseignant → étudiant invité → étudiant créateur) passent en UI réelle avec persistance
  vérifiée après rechargement et reconnexion.
- Fonctionnalités recensées : **24 postes** couvrant 9 domaines (auth/sessions/rôles,
  espaces/invitations, documents/ingestion, persona, chat, fiches/validation, quiz/
  tentatives/corrections, dashboards/analytics, sécurité fonctionnelle) — voir matrice §B.
  - Vérifiés : **23** (dont 4 après correction).
  - Non testés (choix documentés §D) : partage vers groupe, gamification en profondeur,
    mot-de-passe-oublié, suppression d'espace.
  - Absentes par conception (règles produit confirmées) : génération de fiche côté
    enseignant, publication de quiz par un étudiant.
- Scénarios/postes exécutés : **24** (18 workflows + 6 sondes sécurité) : **19** réussis
  du premier coup, **4** débloqués par correction, **1** N/A assumé ; **3** observations
  documentées sans correction (périmètre gelé ou arbitrage produit).
  Demande post-audit : +1 fix layout chat (vérifié, preuves 22–23).
- Problèmes identifiés : **13** — **7 corrigés et re-testés** (1 P1, 4 P2, 2 P3),
  **6 documentés** restants (tous P3).
- Régressions : `infra/scripts/07-test-e2e.sh` **8/8 OK** avant et après correctifs ;
  `npm run typecheck` **OK** ; `04-status.sh` **8/8 UP**.

## B. Matrice des workflows

| # | Workflow (rôles) | Étapes clés | Statut | Preuves | Restes |
|---|---|---|---|---|---|
| A.1 | Inscription/connexion (enseignant) | Formulaire, validation à vide (HTML5 natif), register → onboarding rôle → création espace → session persistée, guard `/`→`/enseignant`, rôle ENSEIGNANT vérifié via API | ✅ | §E | Toast de confirmation absent après enregistrement (P3) |
| A.2 | Espace (enseignant) | Création, renommage + PUT 200 + persistance reload, code invitation affiché | ✅ | 02, 03 | Pas de distinction brouillon/publié pour les espaces (création = visible) |
| A.3 | Document (enseignant) | Drop PDF → `En attente` → `Prêt` (3 segments) ; pipeline vérifié en logs (docling `pymupdf_layout`, 3 chunks, upsert Qdrant, `DOCUMENT_READY`) | ✅ | 03 | — |
| A.4 | Persona (enseignant) | v1 création → **v2 enrichi auto après ingestion** → v3 régénéré manuellement, persisté reload, badge « Persona v3 » dans le chat | ✅ | 04 | — |
| A.5 | Assistant (enseignant) | Q1 2e loi Newton ✅ + 3 citations nommées ; Q2 chute libre ✅ ; Q3 hors-sujet répondue par culture générale sans repli affiché (obs., chat gelé) ; historique persisté reload | ✅ avec 2 obs. | 05 | KaTeX brut (P2 gelé) ; pas de refus hors-sujet (obs.) |
| A.6 | Fiche (enseignant) | **Pas de bouton de génération côté enseignant** (règle produit : génération côté étudiant/membre, validation côté enseignant) | N/A assumé | — | — |
| A.7 | Quiz (enseignant) | Modale 3 étapes (Corpus/Document/Thème, Facile/Moyen/Difficile, 5/10/15/20), génération 201, brouillon → **Publier** → `PUBLIE`, 5 questions fidèles au cours vérifiées via API | ✅ après fix | 06 | Liste ne se rafraîchissait pas (P2 corrigé) ; « 0 document » scope DOCUMENT (P3 corrigé) |
| B.1 | Invitation (ens.→étu.) | Code `GZ35W7S6`, erreur explicite sur code invalide, adhésion → redirection espace, `owner=false` | ✅ | — | — |
| B.2 | Accès étudiant | Onglets sans Paramètres, quiz publié visible après fix, documents du membre listés, droits conservés reload | ✅ après fix | — | — |
| B.3 | Passage quiz (étudiante) | 5 questions, navigation Précédent/Suivant, soumission → **5/5 100 %**, verdicts serveur, tentative persistée (`attempts/mine`) | ✅ après fix | 08 | Avertissement beforeunload après soumission réussie (P3) |
| B.4 | Dashboard étudiant | 100 % par matière, historique « 1 quiz · ~10 min », 0/0 questions/fiches | ✅ | 09 | — |
| B.5 | Dashboard enseignant | 2 actifs, semaine 12/10 = 2, étudiante à 100 %, tentative 5/5 + correction au commentaire persistée | ✅ | 07, 10, 13 | Dates 21/01/1970 (P2 corrigé) ; UUID tronqués (limite connue) |
| C.1 | Espace étudiant (créatrice) | Création autorisée, 2e PDF indépendant READY (2 chunks), corpus isolé de l'espace physique | ✅ | — | — |
| C.2 | Fiche étudiante | Génération Map-Reduce (définition, points clés, exemple, erreurs courantes, auto-quiz fidèles), `sourceDocumentIds=[]` scope SPACE (P3 noté) | ✅ | — | — |
| C.3 | Quiz étudiant | Génération OK (10 questions) ; **publication impossible en tant qu'étudiante** (403, règle produit) | ✅ (règle) | — | L'étudiante ne voit pas ses brouillons dans sa liste (filtre PUBLIE seul, P3) |
| C.4 | Validation (enseignant) | File `fiches-a-valider` transverse, **VALIDEE** persistée (`enseignantId` correct), **REJETEE** 400 sans commentaire / 200 avec, auto-validation étudiante 403 | ✅ | 11 | Tout enseignant voit toutes les fiches (règle large assumée) |
| C.5 | Passage quiz étudiant | Quiz de l'étudiante publié par l'enseignant (cross-user OK) → **10/10**, `tauxReussite=1.0`, `nbFichesGenerees=1` via API | ✅ | 12 | — |
| Sécu | Isolation (6 sondes) | BROUILLON d'autrui 403, publish étudiant 403, sans token 401, espace privé d'autrui 403, tentative sur BROUILLON 400, PUBLIE lisible masqué 200 sans fuite (`type/question/options` seuls) | ✅ | — | Lecture PUBLIE ouverte à tout connecté (arbitrage documenté, §D) |
| Visu | 390 px | Quiz enseignant : pas de chevauchement, titre ellipsis, onglets scrollables | ✅ | 14 | — |

Légende : ✅ vérifié (dont après correction), N/A assumé = règle produit confirmée.

## C. Bugs corrigés

### P1 — Quiz publié illisible par les étudiants (bloquant B.2/B.3)
- Défaut : `GET /quizzes/{id}` → 403 pour tout non-propriétaire même si `PUBLIE` ;
  `GET /quizzes?spaceId=` ne renvoyait que ses propres quiz. Conséquence : un quiz publié
  était impassable (page take « Aucune question », le parseur zod exigeant `answer`).
- Cause : contrôle `assertOwnerOrAdmin` sans exception pour le statut `PUBLIE`, alors que
  l'infrastructure de masquage (`QuizPublicResponse`) existait déjà ; parseur frontend
  incompatible avec la vue masquée ; verdict résultats comparé à `correct_answer` vide.
- Fichiers : `fiche-service/.../service/QuizService.java` (`getById` → vue masquée si
  `PUBLIE`, 403 inchangé sinon ; `listMine` → propres complets + publiés d'autrui masqués),
  `frontend/.../quiz/lib/parse-quiz-content.ts` (`answer` optionnel, `correct_answer=''` par
  défaut, `correct` serveur propagé), `.../types.ts` (`AnswerEntry.correct?`),
  `.../QuizResultsPanel.tsx` (verdict serveur prioritaire),
  `.../QuizQuestionFeedback.tsx` (masque « Bonne réponse »/explication vides),
  `fiche-service/README.md` (contrat documenté).
- Vérification : étudiant lit le PUBLIE en 200 masqué (0 fuite `answer`), passage 5/5,
  BROUILLON d'autrui toujours 403, `07-test-e2e.sh` 8/8, `typecheck` OK.

### P2 — Liste quiz non rafraîchie après génération
- Défaut : après « Générer un quiz », « Aucun quiz généré » persistait jusqu'au reload
  (la fiche, elle, se rafraîchissait).
- Cause : `useGenerateQuiz.onSuccess` n'invalidait pas `quizKeys.espace(spaceId)` (liste
  enseignant) — la mutation publish, elle, invalidait correctement.
- Fichier : `frontend/.../quiz/api/use-generate-quiz.ts` (+1 ligne miroir du hook fiches).
- Vérification : 2e quiz (scope DOCUMENT) apparu en « Brouillons 1 » sans reload.

### P2 — Dates « 21/01/1970 » sur le Pilotage enseignant
- Défaut : `Questions fréquentes` affichait ×1 · 21/01/1970.
- Cause racine : `CommonAutoConfiguration.objectMapper()` remplace le mapper Boot par un
  `new ObjectMapper()` (timestamps activés) dans **tous** les services → `Instant`
  sérialisés en secondes-epoch flottantes, que `new Date()` lit comme des millisecondes.
- Fichier : `common/.../config/CommonAutoConfiguration.java`
  (`disable(WRITE_DATES_AS_TIMESTAMPS)` + commentaire de contrat ISO-8601).
- Vérification : `dernierAsk` ISO en API, « 15/10/2026 » en UI, uniformisé aussi pour
  `meta.timestamp`, `createdAt`, documents ; `07-test-e2e.sh` 8/8 après rebuild complet.

### P3 — « 0 document » sur les quiz de scope DOCUMENT
- Cause : `sourceDocumentIds` jamais renseigné (seul `targetDocumentId` l'était).
- Fichier : `QuizService.generate` (`DOCUMENT` → `sourceDocumentIds=[targetDocumentId]`).
- Vérification : image reconstruite et redéployée (s'applique aux générations futures).

### P2 — Layout chat : input non fixé + grand vide scrollable (demande post-audit)
- Défauts : l'input défilait avec les messages ; un grand fond vide restait scrollable
  (colonne `h-svh` + `overflow-y-auto` sous l'en-tête et les onglets, donc dépassement
  systématique ; double scroll imbriqué).
- Causes : (1) colonne `h-svh` au lieu de remplir la zone d'onglet ; (2) le
  `SidebarProvider` impose `min-h-svh` et grandissait avec le contenu ; (3) l'auto-scroll
  utilisait `bottomRef.scrollIntoView()`, qui défilait aussi les ancêtres
  `overflow:hidden` (`scrollTop=631` mesuré sur la colonne → tout l'écran décalé).
- Fichiers : `frontend/.../shared/ChatPage.tsx` (colonne `h-full min-h-0 overflow-hidden`,
  provider forcé `h-full min-h-0` via tailwind-merge), `frontend/.../shared/SpaceLayout.tsx`
  (verrouillage viewport `h-svh` + `overflow-hidden` **uniquement** sur l'onglet `/chat`,
  autres onglets inchangés), `frontend/.../chat/components/ChatThread.tsx` (`min-h-0`,
  pied fixe `shrink-0 border-t ... pb-4 pt-3`, auto-scroll borné au fil via `scrollTo`).
- Vérification : page 900/900 (zéro scroll), un seul conteneur scrollable (le fil),
  input visible en bas après scroll en haut (preuves 22, 23), onglet Documents et
  `fiches-a-valider` inchangés (scroll de page conservé).

### P2 — Rendu chat : maths `\(...\)`/`\[...\]`, `---`, `<br>` (demande post-audit, gel levé)
- Défauts : formules LaTeX affichées en brut (y compris dans les cellules), `---`
  littéraux (13 sur l'écran DAM), `<br>` littéral dans un tableau, **0 KaTeX**.
- Causes : `ResponseParser` ne découpait que `$$`/`$` ; `MarkdownBlock` sans règle HR,
  sans `<br>`, sans math inline. Première tentative (extraction `\(...\)` en blocs)
  **abandonnée après re-test** : elle fragmentait les lignes de tableau.
- Solution retenue : backend n'extrait en blocs que le **niveau bloc**
  (`\[...\]` isolé sur ses lignes → `MATH_DISPLAY`, pattern ancré `^...$` multiligne) ;
  tout le **niveau inline** (`\(...\)`, `\[...\]` mid-phrase, `<br>`) est rendu par le
  frontend dans le flux (`renderInline` → `MathBlock display={false}` / `<br/>` React,
  sans HTML brut). `---`/`***`/`___` seuls sur leur ligne → `<hr/>`.
- Fichiers : `chat-service/.../ResponseParser.java` (+ pattern + étape 2bis + note
  d'architecture), `ResponseParserTest.java` (+7 tests : isolé vs mid-phrase vs tableau),
  `frontend/.../MessageRenderer.tsx` (règle HR, `<br>`, maths inline en premier dans
  l'alternance, groupes non-capturants pour `split`).
- Vérification : `ResponseParserTest` **26/26** (19 existants + 7 nouveaux) ; reload DAM
  (re-parse rétroactif, sans nouvel appel LLM) : **13 `<hr/>`, 48 KaTeX, 0 source
  résiduelle, tableau 4 lignes intact avec `cases` composé** (preuve 27 reprise) ;
  `07-test-e2e.sh` 8/8 ; `typecheck` OK.

### P3 — En-tête d'espace repliable sur l'onglet chat (demande post-audit)
- Besoin : garder l'en-tête + onglets accessibles mais pouvoir les masquer pour
  agrandir le chat, qui reste inchangé.
- Solution : bouton repli (chevron, `aria-expanded`) dans la rangée titre — visible
  uniquement sur `/chat` — remplaçant l'en-tête + onglets par une barre fine (retour,
  nom tronqué, bouton déplier) ; préférence persistée par espace en `localStorage`.
  Les deux chevrons sont placés **à gauche** (avant le titre / avant le retour) car la
  zone droite est recouverte par le panneau conversations déplié (mesuré : chevron à
  x=1388 sous le rail 1184–1440).
- Fichier : `frontend/.../shared/SpaceLayout.tsx` (aucun impact hors `/chat`).
- Vérification : repli/dépli + persistance reload (preuves 24 desktop, 25 mobile 390 px),
  input toujours fixé, autres onglets inchangés.

## D. Problèmes restants

| # | Gravité | Reproduction | Attendu / réel | Blocage | Next step |
|---|---|---|---|---|---|
| 1 | P2 → **corrigé** (voir §C, gel levé sur demande) | Chat : formules `\(...\)`/`\[...\]`, lignes `---` | KaTeX/`---` rendus / ~~sources brutes affichées~~ **corrigé et vérifié** (preuves 15–19 avant, 27 après : 13 `<hr/>`, 48 KaTeX, 0 résidu, tableau intact) ; annexe §G (matrice initiale) | — | — |
| 2 | Obs. (gelé) | « Qui a peint la Joconde ? » dans l'espace physique | Repli/refus ou cadrage cours / **réponse de culture générale sans mention du cours** (correcte, sans hallucination d'appartenance) | Chat gelé ; garantie produit non établie | Trancher produit : refus hors-sujet ou réponse assumée |
| 3 | P3 | Enregistrer espace, publier quiz, avant/après | Toast de confirmation / **aucun feedback visible** (PUT 200 effectif) | — | Ajouter toasts succès/échec |
| 4 | P3 | Étudiant non-propriétaire sur détail quiz/fiche | Bouton « Partager » masqué / **visible puis 403** | — | Masquer les actions owner-only |
| 5 | P3 | Soumettre un quiz puis naviguer (goto) | Pas d'avertissement / **beforeunload « réponses perdues » alors qu'elles sont soumises** | — | Désenregistrer le garde après submit |
| 6 | P3 | Visiteur non connecté (landing, `/`) | Pas d'erreur console / **401 `users/me` loggé** (sonde de session) | — | Silence applicatif sur 401 de sonde |
| 7 | P3 | Liste quiz étudiante | Section Brouillons / **l'étudiante ne voit pas ses propres brouillons** (filtre PUBLIE seul) | Règle d'affichage | Ajouter section brouillons owner |
| — | Arbitrage | `GET quiz PUBLIE` par tout connecté (même non-membre) | Lecture restreinte aux membres / **ouverte, masquée, UUID imprévisible** ; la soumission était déjà ouverte sans contrôle de membre | Cohérence avec `submit` existant | Si durcissement voulu : contrôle d'appartenance via `SpaceClient` (précédent chat-service) |
| — | Limites connues confirmées | — | UUID tronqués (pas de résolution batch), citations sans nom si chunk d'autrui, `sourceDocumentIds=[]` scope SPACE, `beforeunload` SSE `AsyncRequestTimeoutException` (bénin, reconnect) | Documentées upstream | — |

Fonctionnalités non testées : partage vers groupe, gamification/objectifs en profondeur,
mot-de-passe-oublié, suppression d'espace (destructif — non exécuté par principe).

## E. Preuves

Captures et fixtures : `.artifacts/e2e/` (non versionné, voir `.gitignore`) :
`01-landing-accueil.png`, `02-dashboard-enseignant.png`, `03-espace-document-pret.png`,
`04-persona-version3.png` (régénéré — voir note), `05-chat-reponse-citations.png`,
`06-quiz-publie.png`, `07-pilotage-enseignant.png`, `08-quiz-resultat-5sur5.png`,
`09-dashboard-etudiant-apres-quiz.png`, `10-quiz-tentative-vue-enseignant.png`,
`11-fiche-validee-file-enseignant.png`, `12-quiz-chimie-10sur10.png`,
`13-pilotage-dates-corrigees.png`, `14-mobile-quiz-390.png`,
`15-chat-tableau-math-brut.png`, `16-chat-separateurs-hr-bruts.png`,
`17-chat-katex-dollars-ok.png`, `18-chat-code-liste-ok.png`,
`19-chat-katex-inline-dollar-ok.png`,
`22-chat-input-fixe-bas.png`, `23-chat-input-fixe-haut-ok.png`,
`24-chat-header-replie.png`, `25-chat-header-replie-mobile.png`,
`26-dam-perceptron-rendu-brut.png` (compte étudiant Mahandry, espace DAM :
13 `---` bruts, 1 tableau, **0 KaTeX** sur tout l'écran),
`cours-mecanique-qa.pdf`, `cours-chimie-qa.pdf`.
Logs : `docker logs tsimoka-{service}` (ingestion : `pymupdf_layout`, 3 chunks/2709 car.,
upsert Qdrant, `DOCUMENT_READY` ; chimie : 2 chunks).
IDs : espace physique `19ce33f4-…`, quiz 5Q `8099fe1f-…`, espace chimie `89d58cad-…`,
fiche chimie `8fc301db-…` (VALIDEE), fiche rejet `96c54ca6-…` (REJETEE motivée),
quiz chimie `1d60bc56-…` (10/10), draft DOCUMENT `61802e04-…` (BROUILLON).

## F. État final du dépôt

Modifiés (non committés, à relire avant commit) : `.gitignore` (`.artifacts/`),
`common/.../CommonAutoConfiguration.java`, `fiche-service/.../QuizService.java`,
`fiche-service/README.md`, `chat-service/.../service/ResponseParser.java`,
`chat-service/.../service/ResponseParserTest.java` (+7 tests),
`frontend/.../shared/{ChatPage.tsx, SpaceLayout.tsx}`,
`frontend/.../chat/components/{ChatThread.tsx, MessageRenderer.tsx}`,
`frontend/.../chat/components/ChatThread.tsx`, `frontend/.../quiz/{api/use-generate-quiz.ts,
lib/parse-quiz-content.ts, types.ts, components/QuizResultsPanel.tsx,
components/QuizQuestionFeedback.tsx}` (+ ce rapport).
Images reconstruites et redéployées : `common`→ tous services, `fiche-service`,
`frontend`. Commandes : `docker compose --profile ollama up --build`,
`docker build docling-worker`, `ollama pull nomic-embed-text`,
`./infra/scripts/04-status.sh` (8/8 UP), `./infra/scripts/07-test-e2e.sh` (8/8, ×2),
`npm run typecheck` (OK, ×3). Laissé intact : fichier non suivi du checkpoint
(précédemment committé `2a7b3df`), données préexistantes en base (espaces H3-*, DAM… —
jamais touchées), `docs/captures-memoire/` (snapshots internes de l'outil Playwright,
non suivis, laissés en place).
⚠️ Footgun compose constaté deux fois pendant l'audit : lancer
`docker compose up -d <service>` depuis `frontend/` fait résoudre les `${VAR}` avec
`frontend/.env` (ou vide) au lieu du `.env` racine, et `depends_on: api-gateway`
provoque alors la **recréation de la gateway avec `JWT_SECRET` vide** (crash-loop
`WeakKeyException`, 502). Toujours opérer compose **depuis la racine du dépôt** ;
vérifier via `docker inspect <svc> | grep JWT_SECRET` après tout `up`.

## G. Annexe — vérification du parser chat (15/10, lecture seule)

5 sondes navigateur dans la conversation de l'espace physique (compte enseignant),
aucune modification de code (périmètre gelé).

| Syntaxe testée | Verdict navigateur | Preuve |
|---|---|---|
| `$$...$$` (bloc) | ✅ KaTeX composé (`x(t)=½at²+v₀t+x₀` centrée) | 17 |
| `$...$` (inline) | ✅ 8 éléments `.katex` (`𝐸𝑐=½𝑚𝑣²`, `Δ𝐸𝑐=∑𝑊`) | 19 |
| `\(...\)` / `\[...\]` (délimiteurs LaTeX émis spontanément par Groq) | ❌ source brute dans paragraphes et cellules de tableau | 05, 15, 16, 17 (contraste) |
| `---` sur sa propre ligne | ❌ paragraphe littéral « --- », **0 `<hr/>`** dans le DOM | 16 |
| Tableau Markdown `\|` | ✅ `<table>` + scroll-x, en-têtes et lignes | 15 |
| Listes `-`/`*`/numérotées, gras, italique, code inline, liens, blockquote, titres `#`–`####` | ✅ | 15, 18 et réponses précédentes |
| Bloc de code + label langage + bouton Copier | ✅ (`<pre>`, `python`) | 18 |
| `~~barré~~`, listes `+`, listes imbriquées, titres `#####`+, `***` | ❌ non supportés — établi par lecture de `MessageRenderer.tsx` (regex `MarkdownBlock`), non émis par le LLM pendant les tests | — (code) |
| Mermaid | Non testé live (chemin `CODE`→`MermaidBlock` présent dans le code) | — |

Causes racines identifiées (sans intervenir) :
1. Backend `ResponseParser` (chat-service, `MATH_DISPLAY`/`MATH_INLINE`) ne reconnaît
   que `$$...$$` et `$...$` : les `\(...\)`/`\[...\]` que Groq émet spontanément restent
   dans les blocs `MARKDOWN`, que le frontend ne traite jamais comme des maths.
2. Frontend `MessageRenderer.MarkdownBlock` (parser maison volontairement strict) : aucune
   règle pour les lignes `---`/`***` (tombent en paragraphe littéral), aucune gestion
   math inline, pas de `~~`, pas de listes `+`/imbriquées.
Pistes (hors audit) : étendre les patterns backend à `\(...\)`/`\[...\]` **ou** contraindre
le prompt système à n'émettre que `$`/`$$` ; ajouter `^---\s*$`→`<hr/>` côté frontend.
Observation transverse : la réponse hors-sujet (Joconde) porte une citation
`cours-mecanique-qa.pdf p.1` non pertinente (retrieval cité même sans ancrage).
