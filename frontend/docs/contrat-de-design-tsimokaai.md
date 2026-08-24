# Contrat de Design — TsimokaAI
### Assistant pédagogique intelligent — direction visuelle du frontend Next.js

> Ce document est le pendant visuel du *Cahier des Charges Fonctionnel* et de la page *Base de projet*. Comme les contrats d'interface, de données et de sécurité qui y sont définis, les règles ci-dessous sont **non négociables** une fois le développement du frontend commencé : elles évitent que l'UI dérive vers un template SaaS générique au fil des sprints.

---

## 0. Ancrage — de quoi s'agit-il vraiment ?

Avant la couleur ou la typo, le vrai sujet : **TsimokaAI n'est pas un chatbot avec des fichiers attachés.** C'est un outil qui transforme un cours dispersé (PDF, slides, notes) en deux objets bien distincts :

1. **Une conversation** — vivante, exploratoire, éphémère par nature (le chat RAG).
2. **Une fiche** — figée, structurée, réutilisable (Définition / Points clés / Exemple), l'élément que le CDC identifie explicitement comme le **différenciateur du projet** face à NotebookLM (§1.2, §Vision : *"la production de fiches de révision typées et structurées plutôt qu'un résumé libre"*).

Toute la direction visuelle part de cette dualité fonctionnelle, pas d'une palette choisie à priori.

**Public :** étudiants (usage quotidien, souvent le soir, sur machines modestes) et enseignants (usage ponctuel, configuration d'espace). Le job de chaque écran diffère donc : le chat doit donner l'impression de *penser avec* l'étudiant ; la fiche doit donner l'impression d'un *document qu'on peut sortir en examen*.

---

## 1. Signature — le composant Fiche

L'élément mémorable du produit, décliné partout : dans le chat (citations de sources), dans le dashboard (notions), dans la gamification (badges).

**Métaphore : la fiche bristol de bibliothèque.** Un rectangle net, une languette colorée sur le bord gauche qui reprend le `subject_tag` de l'espace (comme les onglets d'un fichier de bibliothèque), un en-tête en petites capitales, des sections **nommées, pas numérotées** — parce que Définition / Points clés / Exemple ne sont pas une séquence à suivre dans l'ordre, ce sont trois faces d'un même objet. C'est directement la structure `content_json` du `fiche-service` rendue visible.

```
┌──┬─────────────────────────────────────┐
│▐▐│ FICHE · ALGORITHMIQUE S3             │  ← eyebrow en Plex Mono, majuscules
│▐▐│ Complexité des algorithmes de tri    │  ← titre en Fraunces
│▐▐│                                       │
│▐▐│ DÉFINITION                           │  ← label, pas "01"
│▐▐│ ...                                   │
│▐▐│ POINTS CLÉS                          │
│▐▐│ · ...                                 │
│▐▐│ EXEMPLE APPLIQUÉ                     │
│▐▐│ ...                                   │
│▐▐│ ─────────────────────────────────    │
│▐▐│ 3 documents sources · mis à jour hier│  ← footer mono, discret
└──┴─────────────────────────────────────┘
   ↑ languette 8px, couleur = subject_tag
```

Trois variantes du même composant, jamais réinventées ailleurs :
- **Pleine** — génération/consultation de fiche.
- **Chip de citation** — sous un message assistant, pour la traçabilité des chunks (§4.3 du CDC).
- **Sceau/badge** — version circulaire pour la gamification (voir §6).

---

## 2. Les deux surfaces : Ardoise et Papier

Plutôt qu'un mode clair/sombre au choix de l'utilisateur, TsimokaAI a **deux surfaces permanentes, chacune réservée à une fonction précise** — c'est un contrat de design, pas une préférence :

| Surface | Où | Rôle |
|---|---|---|
| **Ardoise** (sombre) | Chat RAG uniquement | zone vivante, conversationnelle, éphémère |
| **Papier** (clair) | Espaces, Fiches, Dashboards, Collaboration | zone structurée, persistante, imprimable |

Cette dualité rend visible, sans texte explicatif, la différence entre *dialoguer avec l'assistant* et *produire un document de révision* — exactement la distinction que fait le CDC en §4.3 vs §4.4.

⚠️ **Auto-critique assumée** : un fond papier proche du cream et une police display serif sont un cliché fréquent des interfaces générées par IA. Le choix reste ici parce qu'il est littéral (une fiche EST un objet papier) et parce que la couleur dominante de la marque reste l'Ardoise, pas le Papier — et parce que l'accent n'est jamais le terracotta générique mais un système d'encres liées aux disciplines (§3).

---

## 3. Palette

6 couleurs nommées, plus un système d'accents fonctionnels réservé aux tags disciplinaires.

### Fond & texte
| Token | Hex | Usage |
|---|---|---|
| `--ardoise-bg` | `#131C1A` | fond du chat, hero, marketing |
| `--ardoise-bg-raised` | `#1B2723` | bulle utilisateur, panneaux sur ardoise |
| `--craie` | `#EDE7D6` | texte sur ardoise |
| `--craie-muted` | `#9FA89F` | texte secondaire sur ardoise |
| `--papier-bg` | `#F2EEDF` | fond des écrans structurés |
| `--papier-carte` | `#FBF9F1` | surface des fiches, cartes |
| `--encre` | `#1D2321` | texte sur papier |
| `--encre-muted` | `#5B645E` | texte secondaire sur papier |

### Tags disciplinaires (`subject_tag`) — contrat de couleur
Ces teintes sont **réservées exclusivement** au marquage disciplinaire (languette de fiche, onglet d'espace, pastille). Interdiction de les réutiliser comme couleur décorative ailleurs dans l'UI — sinon leur valeur informative se dilue.

| Token | Hex | Exemple d'usage |
|---|---|---|
| `--tag-sciences` | `#2F6690` | Maths, Physique, Algo |
| `--tag-info` | `#B8862E` | Informatique, Réseaux |
| `--tag-lettres` | `#9C4A2E` | Lettres, Histoire |
| `--tag-eco` | `#5C7A3A` | Économie, Gestion |
| `--tag-droit-shs` | `#6B4E82` | Droit, Sciences sociales |
| `--tag-langues` | `#A85272` | Langues |

### Système & états
| Token | Hex | Usage |
|---|---|---|
| `--succes` | `#4C7A4E` | fiche validée, badge obtenu |
| `--attention` | `#C08A2E` | fiche obsolète, chapitre difficile |
| `--erreur` | `#A23F3F` | échec d'ingestion, fiche rejetée |

### Dégradé de confiance (progression)
Pas de barre de progression générique rouge/orange/vert. La progression d'une notion (`notions_maitrisees` / `notions_faibles` de l'`analytics-service`) se lit par **densité d'encre**, cohérente avec l'identité papier :

- **Maîtrisée** → texte plein, `--encre` 100%
- **En cours** → `--encre` 60%, soulignement pointillé
- **Fragile** → `--encre` 30%, contour en pointillés + point `--attention`

---

## 4. Typographie

Trois rôles, jamais interchangés — c'est un contrat, pas une suggestion :

| Rôle | Police | Où | Où surtout PAS |
|---|---|---|---|
| **Display** | Fraunces (variable) | titres de fiches, hero, marginalia (italique) | boutons, formulaires, tableaux |
| **Corps / UI** | IBM Plex Sans | chat, dashboard, navigation, boutons | titres de fiches |
| **Utilitaire** | IBM Plex Mono | eyebrows, timestamps, `subject_tag`, `request_id`, `model_used`, codes de badge | corps de texte long |

Les trois se chargent via `next/font/google` (auto-hébergées, zéro dépendance CDN à l'exécution — pertinent vu la contrainte machine modeste/zéro-budget).

### Échelle (rem)
```
--text-xs   0.75/1rem     mono, métadonnées
--text-sm   0.875/1.35rem UI secondaire
--text-base 1/1.6rem      corps de texte
--text-lg   1.125/1.7rem  intro de fiche
--text-xl   1.375/1.8rem  labels de section (DÉFINITION, ...)
--text-2xl  1.75/2.1rem   titres de fiche
--text-3xl  2.25/2.6rem   titres de page
--text-4xl  3/3.4rem      hero (landing uniquement)
```

---

## 5. Espacement, grille, formes

- Base 4px, échelle Tailwind par défaut (4·8·12·16·20·24·32·40·56·72·96).
- Largeur max de lecture : **720px** pour le chat (ligne de lecture confortable), **1120px** pour dashboard/fiches.
- Gouttière mobile 16px, desktop 64px.
- Padding interne des fiches : 24px (32px en desktop).
- **Rayon de bordure volontairement bas** : 2px sur les fiches (effet carton/papier réel, pas de `rounded-2xl` SaaS), 4px sur les boutons. C'est un choix délibéré pour se démarquer du langage "carte arrondie" par défaut.
- Languette de fiche : 8px de large, pleine hauteur, couleur `subject_tag`.

---

## 6. Application par écran

**Liste des espaces ("Étagère")** — les espaces de cours s'affichent comme des dos de reliure verticaux colorés par `subject_tag`, façon étagère de classeurs. Sur mobile : bande horizontale scrollable de chips colorées.

**Chat RAG (Ardoise)** — pas de bulles pour l'assistant : le texte apparaît directement en `--craie` sur le fond ardoise, comme écrit au tableau. Le message utilisateur seul reçoit un panneau `--ardoise-bg-raised`, aligné à droite. Citations de sources = mini-chips Fiche sous la réponse, numérotées `[1] [2]` — réponse à l'exigence de traçabilité du CDC §4.3.

**Génération / consultation de fiche (Papier)** — composant Fiche plein format. Badge `--attention` "obsolète" si un document a été ingéré après génération (§4.4 du CDC).

**Dashboard étudiant** — notions affichées avec le dégradé de confiance (§3). Suivi hebdomadaire = bande de points d'encre pleins/creux plutôt qu'un emoji flamme. Matières faibles mises en avant par une languette `--attention`.

**Dashboard enseignant** — chapitres difficiles listés avec une barre de densité d'encre (plus sombre = plus difficile) plutôt qu'un rouge/vert alarmiste — reste cohérent avec l'identité monochrome + accents disciplinaires.

**Collaboration** — une fiche partagée reçoit un pli de coin (corner-fold CSS) avec la mention du groupe. Les annotations s'affichent en marge, en Fraunces italique, comme une note manuscrite. La validation enseignant est un tampon `VALIDÉE` / `À REVOIR` en Plex Mono majuscules, légèrement pivoté, en `--succes` ou `--attention`.

**Gamification** — badges = sceaux circulaires (mini-Fiche circulaire) dans les couleurs du système. Objectifs de révision = liste façon feuillet à cocher. Rappels = toast façon post-it qui se détache d'un coin d'écran, non intrusif.

---

## 7. Mouvement

Un seul moment orchestré vaut mieux que des micro-animations éparpillées :

- **Streaming de réponse** (Ardoise) : révélation du texte mot par mot, léger fondu + décalage 2px, ~120ms d'intervalle — évoque la craie qui trace. Fallback instantané si `prefers-reduced-motion`.
- **Apparition d'une fiche générée** (Papier) : léger dépôt (translateY 8px → 0, 200ms), une seule fois à la génération — pas de replay au scroll.
- Tout le reste : transitions CSS discrètes (150–200ms), pas de librairie d'animation lourde. Pertinent aussi pour la machine de dev (i5-6200U, pas de GPU) : rester sur des transitions CSS natives plutôt que sur du JS/WebGL évite de plomber le rendu en local.

---

## 8. Accessibilité — plancher de qualité

- Contraste AA minimum vérifié sur toutes les paires texte/fond ci-dessus (craie/ardoise et encre/papier sont toutes deux > 8:1).
- Focus clavier visible partout : contour en pointillés 2px façon trait de craie, jamais supprimé par un `outline: none` sans remplacement.
- `prefers-reduced-motion` respecté : désactive le fondu chalk et le dépôt de fiche, garde les transitions instantanées.
- Mobile-first, testé jusqu'à 360px de large.

---

## 9. Contrat de design — règles non négociables

1. **Contrat de composant** — toute représentation d'une fiche (chat, dashboard, badge, partage) réutilise le composant `Fiche`. Aucune carte ad hoc ne doit réimplémenter sa structure.
2. **Contrat de surface** — Ardoise = chat uniquement. Papier = tout le reste. Ne jamais inverser ni mélanger les deux fonds sur un même écran.
3. **Contrat de couleur** — les tokens `--tag-*` sont réservés au `subject_tag`. Jamais utilisés comme couleur décorative libre.
4. **Contrat de typographie** — Fraunces réservé aux titres de fiches, hero et marginalia. Jamais pour boutons, formulaires ou tableaux (Plex Sans uniquement).
5. **Contrat de mouvement** — aucune animation critique (envoi de message, génération de fiche) au-delà de 300ms. `prefers-reduced-motion` toujours respecté.
6. **Contrat d'accessibilité** — contraste AA minimum, focus visible sur 100% des éléments interactifs.

---

## 10. Feuille de route d'implémentation frontend

1. **Tokens** — déclarer les CSS variables ci-dessus + config Tailwind (`tailwind.config.ts`) + `next/font/google` pour Fraunces / IBM Plex Sans / IBM Plex Mono.
2. **Composant `Fiche`** en premier (réutilisé partout) — variantes `full`, `citation-chip`, `badge-seal`.
3. **Layout Ardoise** (chat) avec le streaming façon craie.
4. **Layout Papier** (espaces, dashboards) avec l'étagère d'espaces et le dégradé de confiance.
5. **Passe accessibilité** — focus, contrastes, reduced-motion.
6. **Gamification & collaboration** — sceaux, pli de coin, tampon de validation — en dernier, ce sont des couches décoratives sur des composants déjà stables.

### Aperçu Tailwind (point de départ)

```ts
// tailwind.config.ts (extrait)
export default {
  theme: {
    extend: {
      colors: {
        ardoise: { DEFAULT: '#131C1A', raised: '#1B2723' },
        craie: { DEFAULT: '#EDE7D6', muted: '#9FA89F' },
        papier: { DEFAULT: '#F2EEDF', carte: '#FBF9F1' },
        encre: { DEFAULT: '#1D2321', muted: '#5B645E' },
        tag: {
          sciences: '#2F6690', info: '#B8862E', lettres: '#9C4A2E',
          eco: '#5C7A3A', droitshs: '#6B4E82', langues: '#A85272',
        },
        succes: '#4C7A4E', attention: '#C08A2E', erreur: '#A23F3F',
      },
      borderRadius: { fiche: '2px' },
      fontFamily: {
        display: ['var(--font-fraunces)'],
        sans: ['var(--font-plex-sans)'],
        mono: ['var(--font-plex-mono)'],
      },
    },
  },
}
```

---

*Ce document complète, sans le remplacer, le Cahier des Charges Fonctionnel et la Base de projet. Toute évolution significative de la direction visuelle doit y être reportée, au même titre qu'une évolution du périmètre fonctionnel.*
