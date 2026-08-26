#!/usr/bin/env bash
# ============================================================================
# Démarrage de ROUTINE : images déjà construites, on (re)monte les conteneurs.
#
#   ./infra/scripts/02-start.sh                 # sans le LLM local ollama
#                                               # (chat via Groq/Gemini selon .env)
#   ./infra/scripts/02-start.sh --profile-ollama  # inclut le LLM local
#                                                   # (requis pour l'ingestion)
#
# NB : l'ingestion de documents a besoin d'ollama (embeddings locaux) — en cas
# de doute, passer --profile-ollama.
# ============================================================================
source "$(dirname "$0")/_lib.sh"

PROFILE_ARGS=()
[ "${1:-}" = "--profile-ollama" ] && PROFILE_ARGS=(--profile ollama)

env_file_present || fail ".env absent — lancer ./infra/scripts/01-premier-demarrage.sh d'abord."

info "Démarrage des conteneurs"
docker compose "${PROFILE_ARGS[@]}" up -d

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

"$REPO_ROOT/infra/scripts/04-status.sh"
ok "Plateforme prête : http://localhost:3000"
