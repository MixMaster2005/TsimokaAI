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
# Usage : ./infra/scripts/01-premier-demarrage.sh [--with-chat-model]
# ============================================================================
source "$(dirname "$0")/_lib.sh"

WITH_CHAT_MODEL=false
[ "${1:-}" = "--with-chat-model" ] && WITH_CHAT_MODEL=true

env_file_present || fail ".env absent à la racine — cp .env.example .env puis compléter."

"$REPO_ROOT/infra/scripts/00-prerequis.sh" || true

info "Construction du conteneur docling-worker"
docker build -t docling-worker:latest "$REPO_ROOT/docling-worker"

info "Démarrage complet de la stack (profil ollama inclus)"
docker compose --profile ollama up -d --build

info "Chargement du modèle d'embedding (nomic-embed-text)"
docker exec tsimoka-ollama ollama pull nomic-embed-text

if [ "$WITH_CHAT_MODEL" = true ]; then
    info "Chargement du modèle de chat local (qwen2.5:3b)"
    docker exec tsimoka-ollama ollama pull qwen2.5:3b
fi

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

ok "Plateforme prête : http://localhost:3000"
info "Tester : ./infra/scripts/07-test-e2e.sh"
