# TsimokaAI — Frontend

Scaffold React (SPA, Vite) + TanStack Router (routing par fichiers) + TanStack Query + shadcn/ui.

## Démarrer

```bash
npm install
cp .env.example .env   # ajuster VITE_API_BASE_URL si besoin
npm run dev
```

`npm run build` fait tourner `tsc -b` (type-check strict) puis `vite build` — les deux passent sans erreur sur ce scaffold.

## Ce qui est réellement câblé (pas juste stub)

Vérifié contre le vrai code du repo backend (`github.com/MixMaster2005/TsimokaAI`) pendant le scaffolding, pas deviné :

- **auth** — login/register/session/update-profile/delete-account, sur les vraies routes de `user-service`
- **espaces** — CRUD complet, `SpineCard`/`EtagereGrid`/`CreateEspaceModal`, plus `get-tag-color.ts` qui résout le fait que `subjectTag` est un `String` libre côté back (pas un enum)
- **chat** — conversations, messages, envoi (avec ajout optimiste du message utilisateur + effet "craie" client-side), sur les vraies routes de `chat-service`
- **fiches** — génération, liste, détail, composant signature `FicheCard` (+ variantes chip/sceau), sur `fiche-service` — y compris le vrai format `content_json` (`definition`/`key_points`/`example`)
- **documents** — upload multipart, liste avec statut, polling automatique tant qu'un document n'est pas `READY`/`FAILED`
- **groupes** — liste + création, sur `space-service`
- **objectifs** / **gamification** (badges, rappels) — câblés sur les vrais DTO, avec les vraies valeurs d'enum (voir plus bas)
- **dashboard étudiant** — progression, recommandations, taux de réussite par matière (agrégé via `useQueries` sur tous les espaces, cf. note dans `use-student-dashboard.ts`)
- **Layout Espace** (Chat/Fiches/Documents/Membres/Paramètres) et **Layout App Étudiant** en entier

## Corrections apportées à la doc existante (Notion)

En vérifiant le code réel, deux divergences trouvées avec la cartographie UI / le contrat de design déjà écrits — **à corriger côté Notion** :

1. `ObjectifRevision.Statut` réel : `EN_COURS / ATTEINT / ABANDONNE` — pas `EXPIRE`.
2. `Recommandation.Type` réel : `REVISION_NOTION_FAIBLE / CHAPITRE_DIFFICILE / RELANCE_INACTIVITE` — pas `NOTION_A_REVOIR / CHAPITRE_A_RETRAVAILLER / CONSEIL_PERSONNALISE`.

## Limitations backend découvertes pendant le scaffolding (pas des bugs front)

- **"Mes fiches" transverse** (`routes/_app/mes-fiches.tsx`) : `FicheController.listMine` exige un `spaceId`, aucun endpoint "toutes mes fiches tous espaces". Page non implémentable en l'état — voir le commentaire en tête de fichier pour les deux options d'ajout côté back.
- **Rejoindre un espace par code** : aucune route de ce type repérée dans `SpaceController` — bouton désactivé dans `onboarding/bienvenue.tsx`.
- **Citations du chat** : `Message.retrievedChunkIds` ne donne que des UUID de chunks, pas de nom de document lisible — aucun endpoint de résolution repéré dans `ingestion-service`. `CitationChips.tsx` affiche un placeholder en attendant.
- **`/` partagé par deux layouts** : `_app` et `_public` sont tous deux pathless, donc en conflit sur `/`. Résolu en laissant `/` à l'étagère (app) et en déplaçant la Landing à `/accueil` — voir le commentaire dans `routes/_public/route.tsx`, ce compromis vaut le coup d'être rediscuté avant un vrai lancement public.

## Ce qui n'a délibérément pas été fait

- **Layout App — Enseignant** (sidebar différente, cf. cartographie UI C.2) : pas commencé, le rôle ADMIN n'est pas géré côté front pour l'instant (`beforeLoad` de `_app` ne vérifie que la présence d'une session, pas le rôle).
- **Partage de fiche / annotations / validation enseignant** : DTO repérés (`ShareFicheRequest`, `ValidationController`) mais pas branchés — actions notées en commentaire dans `fiches/$ficheId.tsx`.
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
├── features/        # logique métier par domaine (api/, components/, types.ts, lib/)
├── components/
│   ├── ui/          # shadcn — ne pas éditer en profondeur, wrap au lieu de modifier
│   └── shared/       # transverse à ≥2 features (AppSidebar)
├── lib/             # api-client, query-client, utils (cn)
└── styles/globals.css  # tokens du contrat de design → variables shadcn, .surface-ardoise
```

Détail des conventions (query key factories, quand extraire un composant, `.surface-ardoise`, etc.) : voir la conversation qui a précédé ce scaffold, ou directement les commentaires en tête de chaque fichier de `lib/` et `features/*/api/keys.ts`.

## Prochaines étapes suggérées

1. Régénérer `components/ui/` via la vraie CLI shadcn.
2. Trancher les 3 limitations backend ci-dessus (mes-fiches, join by code, citations lisibles) — ce sont des décisions produit/back, pas du travail front.
3. Layout App — Enseignant.
4. Partage de fiche + annotations + validation enseignant.
5. Générer `types/api.d.ts` depuis un futur schéma OpenAPI (springdoc côté back + `openapi-typescript` côté front) pour ne plus avoir à vérifier les DTO à la main comme pendant ce scaffolding.
