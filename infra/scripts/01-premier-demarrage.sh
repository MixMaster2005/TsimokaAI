#!/usr/bin/env bash
# ============================================================================
# PREMIER démarrage de la plateforme (idempotent : relancer ne casse rien).
#
# Fait, dans l'ordre :
#   1. Vérification des prérequis (00-prerequis.sh).
#   2. Construction de l'image docling-worker (extraction de documents, spawnée
#      à la demande par ingestion-service — pas un service Compose permanent).
#   3. Démarrage de TOUTE la stack, ollama inclus (profil "ollama").
#   4. Chargement du modèle d'embedding nomic-embed-text dans le conteneur
#      ollama — requis par ingestion-service pour indexer les documents.
#      Le modèle de chat local qwen2.5:3b n'est utile que si
#      ACTIVE_LLM_PROVIDER=ollama (chargé seulement sur demande : --with-chat-model).
#   5. Attente du démarrage effectif des 7 services Spring.
#
# Usage :
#   ./infra/scripts/01-premier-demarrage.sh                      # tout (défaut)
#   ./infra/scripts/01-premier-demarrage.sh --no-build           # sans rebuild (images déjà construites)
#   ./infra/scripts/01-premier-demarrage.sh --no-frontend        # sans build/lancement du frontend (dev backend)
#   ./infra/scripts/01-premier-demarrage.sh --with-chat-model    # charge aussi le modèle de chat local qwen2.5:3b
#   (les options sont combinables, ex: --no-build --no-frontend)
# ============================================================================
source "$(dirname "$0")/_lib.sh"

WITH_CHAT_MODEL=false
NO_BUILD=false
NO_FRONTEND=false
for opt in "$@"; do
    case "$opt" in
        --with-chat-model) WITH_CHAT_MODEL=true ;;
        --no-build)        NO_BUILD=true ;;
        --no-frontend)     NO_FRONTEND=true ;;
        *) warn "Option inconnue ignorée : $opt" ;;
    esac
done

BACKEND_SERVICES="postgres redis qdrant minio ollama api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service"

env_file_present || fail ".env absent à la racine — cp .env.example .env puis compléter."

"$REPO_ROOT/infra/scripts/00-prerequis.sh" || true

if [ "$NO_BUILD" = true ]; then
    info "Build ignoré (--no-build) : images déjà construites attendues"
else
    info "Construction du conteneur docling-worker"
    docker build -t docling-worker:latest "$REPO_ROOT/docling-worker"
fi

BUILD_ARG=()
[ "$NO_BUILD" = false ] && BUILD_ARG=(--build)

if [ "$NO_FRONTEND" = true ]; then
    info "Démarrage du backend uniquement (profil ollama inclus, sans le frontend)"
    docker compose --profile ollama up -d "${BUILD_ARG[@]}" $BACKEND_SERVICES
else
    info "Démarrage complet de la stack (profil ollama inclus)"
    docker compose --profile ollama up -d "${BUILD_ARG[@]}"
fi

info "Chargement du modèle d'embedding (nomic-embed-text)"
docker exec tsimoka-ollama ollama pull nomic-embed-text

if [ "$WITH_CHAT_MODEL" = true ]; then
    info "Chargement du modèle de chat local (qwen2.5:3b)"
    docker exec tsimoka-ollama ollama pull qwen2.5:3b
fi

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

if [ "$NO_FRONTEND" = true ]; then
    ok "Backend prêt"
    info "Frontend à lancer séparément : cd frontend && npm run dev:api  (Vite :5173, proxy /api -> :8080)"
else
    ok "Plateforme prête : http://localhost:3000"
fi
info "Tester : ./infra/scripts/07-test-e2e.sh"
