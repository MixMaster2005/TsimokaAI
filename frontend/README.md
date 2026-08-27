# TsimokaAI — Frontend

SPA React (Vite) + TanStack Router (routing par fichiers) + TanStack Query + shadcn/ui.
Deux applications dans un seul bundle : **App Étudiant** (`_app`) et **App Enseignant**
(`enseignant`, rôle ADMIN), chacune avec son layout, sa sidebar et son guard.

## Démarrer

```bash
npm install
cp .env.example .env   # ajuster VITE_API_BASE_URL si besoin
npm run dev
```

`npm run build` fait tourner `tsc -b` (type-check strict) puis `vite build`.

## Docker

Le service `frontend` de la racine construit ce dossier via un Dockerfile multi-stage :
build Vite dans node, puis nginx sert le bundle et **proxifie `/api` vers api-gateway**.
Conséquence : `VITE_API_BASE_URL` est vide au build → requêtes relatives, même origine,
zéro CORS. Le fallback SPA (refresh sur une URL profonde → `index.html`) est géré par
nginx. Port exposé : **3000**.

```bash
docker compose up --build frontend
```

## Ce qui est réellement câblé (pas juste stub)

Contrats vérifiés contre le code réel des services backend :

- **auth** — login/register/session/update-profile/delete-account, sur les vraies routes de `user-service`
- **espaces** — CRUD complet, `SpineCard`/`EtagereGrid`/`CreateEspaceModal`, plus `get-tag-color.ts` qui résout le fait que `subjectTag` est un `String` libre côté back (pas un enum)
- **adhésion par code** — bouton d'onboarding actif : `JoinEspaceModal` (POST `/api/v1/spaces/join`), page Membres avec code d'invitation du propriétaire (copier/régénérer), retrait de membre, quitter l'espace ; `SpaceResponse.owner` distingue posséder / rejoindre
- **chat** — conversations, messages, envoi (avec ajout optimiste + effet "craie" client-side), sur les vraies routes de `chat-service`
- **citations chat** — `MessageResponse.citations` (document source + extrait, persistés à la génération côté back) affichées par `CitationChips` ; repli placeholder sur les anciens messages (UUID bruts seulement)
- **fiches** — génération, liste, détail, composant signature `FicheCard` (+ variantes chip/sceau), sur `fiche-service` — y compris le vrai format `content_json` (`definition`/`key_points`/`example`)
- **mes fiches transverse** — page branchée sur `GET /api/v1/fiches/mine` (vue tous espaces confondus, tri par date de génération)
- **actions fiche** — partage (`ShareFicheModal` vers groupe ou membre de l'espace), annotations (liste + ajout), validation enseignante (tampon VALIDÉE / À REVOIR, verdict réservé ADMIN)
- **documents** — upload multipart, liste avec statut, polling automatique tant qu'un document n'est pas `READY`/`FAILED`
- **groupes** — liste + création, sur `space-service`
- **objectifs** / **gamification** (badges, rappels) — câblés sur les vrais DTO, avec les vraies valeurs d'enum (voir plus bas)
- **dashboard étudiant** — progression, recommandations, taux de réussite par matière (agrégé via `useQueries` sur tous les espaces, cf. note dans `use-student-dashboard.ts`)
- **Layout App Étudiant** en entier + **Layout App Enseignant v1** (cf. plus bas)

## Corrections apportées à la doc existante (Notion)

En vérifiant le code réel, deux divergences trouvées avec la cartographie UI / le contrat de design déjà écrits — **à corriger côté Notion** :

1. `ObjectifRevision.Statut` réel : `EN_COURS / ATTEINT / ABANDONNE` — pas `EXPIRE`.
2. `Recommandation.Type` réel : `REVISION_NOTION_FAIBLE / CHAPITRE_DIFFICILE / RELANCE_INACTIVITE` — pas `NOTION_A_REVOIR / CHAPITRE_A_RETRAVAILLER / CONSEIL_PERSONNALISE`.

## Layout App — Enseignant (v1)

`routes/enseignant/` + `AppSidebarEnseignant`, rôle ADMIN uniquement. Guards bilatéraux :
un ADMIN sous `_app` repart vers `/enseignant`, un étudiant sous `/enseignant` repart vers
`/`. Parcours : tableau de bord (tous les espaces, `GET /api/v1/spaces/all`) → fiches d'un
espace (`GET /api/v1/fiches/espace/{spaceId}`) → détail avec verdict (tampon du contrat de
design). Ces deux endpoints ont été ajoutés côté back : sans eux, aucun enseignant ne peut
découvrir quoi que ce soit (les espaces sont privés).

Limite : les agrégats "chapitres difficiles avec densité d'encre" du contrat de design
nécessitent des endpoints analytiques non implémentés côté analytics-service.

## Limitations connues (réelles, pas des stubs)

- **Pas de nom lisible hors session** : user-service n'expose pas de résolution batch
  d'utilisateurs — membres d'un espace, annotations et partages affichent des UUID tronqués.
- **Citations sans nom possible** : la résolution du nom de document se fait côté
  chat-service avec l'identité de l'utilisateur de la conversation ; si le chunk vient d'un
  document déposé par un AUTRE membre, ingestion-service répond 403 → citation affichée
  sans nom de fichier (l'extrait reste là).
- **Refresh silencieux non branché** : TODO dans `lib/api-client.ts` (401) et
  `features/auth/api/use-login.ts`.
- **GenerateFicheModal** (choix du périmètre de génération) reste à écrire — le bouton
  génère sur tout le corpus de l'espace.
- **`/` partagé par deux layouts** : `_app` et `_public` sont tous deux pathless, donc en
  conflit sur `/`. La Landing reste à `/accueil` ; un visiteur non connecté qui arrive sur
  `/` y est redirigé, tandis qu'un étudiant déjà connecté retrouve son étagère. À long terme,
  il sera préférable de donner un préfixe explicite à l'application authentifiée (par exemple
  `/app`) afin que la Landing puisse devenir la vraie route canonique `/`.

## Ce qui n'a délibérément pas été fait

- **shadcn/ui** : composants écrits à la main (voir plus bas), pas générés par la vraie CLI.

## shadcn/ui — écrit à la main, à régénérer

`ui.shadcn.com` n'était pas joignable depuis l'environnement où ce scaffold a été construit. Les composants dans `src/components/ui/` suivent les conventions actuelles (style `new-york`, attributs `data-slot`) mais n'ont **pas** été générés par la CLI officielle. Recommandé avant de construire dessus :

```bash
npx shadcn@latest add button card input label tabs dialog avatar dropdown-menu separator badge --overwrite
```

`components.json` est déjà configuré avec les bons alias.

## Structure

```
src/
├── routes/          # WIRING UNIQUEMENT (loader, validateSearch, composition) — voir chaque fichier
│   ├── _app/        # app étudiant (pathless, guard auth + anti-ADMIN)
│   ├── enseignant/  # app enseignant (PRÉFIXÉ — pas pathless, sinon conflit de chemins avec _app)
│   └── _public/     # landing, connexion, inscription
├── features/        # logique métier par domaine (api/, components/, types.ts, lib/)
├── components/
│   ├── ui/          # shadcn — ne pas éditer en profondeur, wrap au lieu de modifier
│   └── shared/      # transverse à ≥2 features (AppSidebar, AppSidebarEnseignant)
├── lib/             # api-client, query-client, utils (cn)
└── styles/globals.css  # tokens du contrat de design → variables shadcn, .surface-ardoise
```

Détail des conventions (query key factories, quand extraire un composant, `.surface-ardoise`, etc.) : voir les commentaires en tête de chaque fichier de `lib/` et `features/*/api/keys.ts`.

## Prochaines étapes suggérées

1. Régénérer `components/ui/` via la vraie CLI shadcn.
2. Brancher le refresh silencieux du token (401 → POST `/api/v1/auth/refresh` → replay).
3. Résoudre les noms d'utilisateurs (endpoint batch user-service) pour remplacer les UUID tronqués.
4. Agrégats enseignant côté analytics-service (chapitres difficiles, densité d'encre).
5. Générer `types/api.d.ts` depuis un futur schéma OpenAPI (springdoc côté back + `openapi-typescript` côté front) pour ne plus avoir à vérifier les DTO à la main.
