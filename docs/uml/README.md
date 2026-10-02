# Diagrammes UML (niveau conception, mémoire)

Habillage commun : Helvetica 12, noir sur blanc, traits 0,75pt, sans ombre.
Libellés en français sans accents (compatibilité moteurs). Aucun nom de
microservice, d'endpoint, de table ou de techno dans les libellés.

| Fichier | Figure mémoire | Contenu |
|---|---|---|
| `cas-utilisation.puml` (+ `.svg` / `.png`) | Cas d'utilisation | 2 acteurs (Étudiant, Enseignant), 4 packages, `<<include>>` / `<<extend>>` |
| `activite-parcours.puml` (+ `.svg` / `.png`) | Workflow principal | 3 couloirs (Étudiant / TsimokaAI / Enseignant), 2 boucles, gardes métier réelles |
| `composants-services.puml` (+ `.svg` / `.png`) | Composants et relations | 8 composants aux noms conceptuels (table de correspondance ci-dessous), flèches pleines/pointillées ; **à insérer en page paysage** |
| `classes/` | Entités métier (4 planches) | Une planche par domaine ; classes externes rappelées hors package, sans package externe affiché |

## Planches classes

| Planche | Domaine | Rappels externes |
|---|---|---|
| `classes/classes-espaces` | Comptes et espaces (référence) | aucun |
| `classes/classes-contenu` | Contenu et assistant | Espace, Utilisateur, Role |
| `classes/classes-revision` | Révision (fiches/quiz) | Groupe, Utilisateur |
| `classes/classes-suivi` | Suivi et motivation | Utilisateur, Espace |

Choix assumés (à justifier dans le mémoire) :
- Attributs métier seuls : ni identifiants techniques, ni horodatages, ni blobs
  (`contentJson`), ni compteurs recalculables, ni agrégats analytiques.
- Exclus : plomberie (`RefreshToken`), découpage RAG (`Chunk`, images), `storageUrl`.
- Associations conceptuelles : le code ne déclare aucune FK JPA inter-services
  (références logiques par UUID) — les liens du diagramme sont conceptuels.
- `PartageFiche` / `QuizShare` fusionnés en `Partage` ; `BadgeObtenu` en association `obtient`.

## Correspondance des noms de composants (hors diagramme)

| Nom conceptuel (diagramme) | Service du dépôt |
|---|---|
| Portail web | `frontend` |
| Gestion des identités | `user-service` |
| Gestion des espaces | `space-service` |
| Ingestion des contenus | `ingestion-service` (extraction internalisée) |
| Assistant conversationnel | `chat-service` |
| Atelier de révision | `fiche-service` (fiches + quiz) |
| Suivi des apprentissages | `analytics-service` |
| Motivation et engagement | `gamification-service` |

Hors champ du diagramme : passerelle d'accès (`api-gateway`) et librairies
partagées (`common`, `ai-common`). Relations vérifiées dans le code : 2 appels
internes (assistant → espaces pour le persona, assistant → ingestion pour les
figures) et 5 flux de notifications asynchrones (compte supprimé, espace
supprimé, document prêt, message envoyé, fiche et quiz).

## Régénérer

```bash
python3 /home/solidarnum/.agents/skills/plantuml/scripts/generate_plantuml.py docs/uml/cas-utilisation.puml docs/uml --format svg
python3 /home/solidarnum/.agents/skills/plantuml/scripts/generate_plantuml.py docs/uml/activite-parcours.puml docs/uml --format svg
python3 /home/solidarnum/.agents/skills/plantuml/scripts/generate_plantuml.py docs/uml/classes docs/uml/classes --format svg
```

Même commandes avec `--format png` pour l'insertion bureautique.
