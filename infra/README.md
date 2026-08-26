# infra/

Infrastructure de la plateforme : initialisation PostgreSQL (`postgres-init/`)
et **scripts d'exploitation** (`scripts/`) — tout ce qu'il faut pour lancer,
observer, dépanner et tester l'application sans retenir de commandes Docker.

## Les scripts

À lancer depuis n'importe quel répertoire (ils se replacent à la racine du
projet eux-mêmes). L'ordre numérique suit le cycle de vie.

| Script | Rôle |
|---|---|
| `scripts/00-prerequis.sh` | Vérifie Docker/Compose, le `.env` (variables obligatoires), les ports hôtes et l'image `docling-worker`. Lecture seule. |
| `scripts/01-premier-demarrage.sh` | Première installation complète : build docling-worker, stack entière (profil `ollama` inclus), chargement de `nomic-embed-text`, attente des 7 services. Idempotent. |
| `scripts/02-start.sh` | Démarrage de routine (images déjà construites). `--profile-ollama` pour inclure le LLM local — requis pour l'ingestion. |
| `scripts/03-stop.sh` | Arrêt. Les volumes (données) sont conservés. `--purge` supprime en plus conteneurs/réseaux orphelins, jamais les volumes. |
| `scripts/04-status.sh` | Santé : liste des conteneurs + `/actuator/health` de chaque service interrogé dans le réseau Docker. Code retour = nombre de services KO. |
| `scripts/05-logs.sh <svc>|all [--tail]` | Logs d'un service ou de toute la stack (follow par défaut). |
| `scripts/06-service.sh <svc> <action>` | Gestion unitaire : `start`, `stop`, `restart`, `rebuild` (recompile l'image Maven dans Docker puis attend le démarrage), `logs`, `sh`. |
| `scripts/07-test-e2e.sh` | Test de bout en bout via la gateway uniquement (comme le frontend) : auth → espace + persona LLM → invitation/adhésion → chat RAG → fiches. Ré-exécutable sans collision (utilisateurs suffixés par timestamp). |

## Parcours types

```bash
# Machine neuve / première fois
./infra/scripts/00-prerequis.sh
./infra/scripts/01-premier-demarrage.sh
./infra/scripts/07-test-e2e.sh

# Tous les jours
./infra/scripts/02-start.sh --profile-ollama   # ou 02-start.sh si pas d'ingestion prévue
./infra/scripts/07-test-e2e.sh                 # vérifier que tout répond
./infra/scripts/04-status.sh                   # état instantané
./infra/scripts/03-stop.sh                     # soir

# Débugger un service
./infra/scripts/06-service.sh chat-service logs
./infra/scripts/06-service.sh chat-service rebuild   # reprend le code Java modifié
./infra/scripts/05-logs.sh all --tail                # vue d'ensemble
```

## Notes

- **`.env` obligatoire** : copier `.env.example` à la racine puis renseigner au
  minimum `JWT_SECRET` et `ACTIVE_LLM_PROVIDER` (+ les clés du provider choisi).
- **Modèle d'embedding** : `01-premier-demarrage.sh` charge `nomic-embed-text`
  dans le conteneur ollama ; sans lui, tout upload de document échoue.
  Ajouter le modèle de chat local (`qwen2.5:3b`) seulement si
  `ACTIVE_LLM_PROVIDER=ollama` (option `--with-chat-model`).
- **Port Ollama publié sur l'hôte : 11435** (et non 11434) pour ne jamais entrer
  en conflit avec un éventuel Ollama installé sur la machine. Les services
  conteneurisés passent par le réseau interne Compose (`http://ollama:11434`).
- **PostgreSQL hôte publié sur 5433** (même raisonnement). À l'intérieur du
  réseau Compose, les services utilisent `jdbc:postgresql://postgres:5432/...`.
- Après un `rebuild`/`restart` d'un service, la gateway peut garder quelques
  instants une connexion périmée vers l'ancien conteneur — relancer
  `docker compose restart api-gateway` si un `UPSTREAM_SERVICE_ERROR` persiste.
