#!/usr/bin/env bash
# ============================================================================
# Démarrage du backend uniquement (sans le frontend Docker) pour un workflow
# dev rapide : le frontend tourne sur l'hôte via Vite (npm run dev:api).
#
#   ./infra/scripts/08-dev-backend.sh                 # sans ollama
#   ./infra/scripts/08-dev-backend.sh --profile-ollama  # avec le LLM local
#
# Une fois le backend up, lancer le frontend dans un autre terminal :
#   cd frontend && npm run dev:api
#
# Le proxy Vite (/api -> localhost:8080) élimine tout CORS — même comportement
# que nginx en prod.
# ============================================================================
source "$(dirname "$0")/_lib.sh"

PROFILE_ARGS=()
[ "${1:-}" = "--profile-ollama" ] && PROFILE_ARGS=(--profile ollama)

env_file_present || fail ".env absent — lancer ./infra/scripts/01-premier-demarrage.sh d'abord."

# Services backend à démarrer (tout sauf frontend)
BACKEND_SERVICES=(
    postgres redis qdrant minio
    api-gateway user-service space-service ingestion-service
    chat-service fiche-service analytics-service gamification-service
)

info "Démarrage du backend (sans frontend Docker)"
docker compose "${PROFILE_ARGS[@]}" up -d "${BACKEND_SERVICES[@]}"

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

"$REPO_ROOT/infra/scripts/04-status.sh"

cat <<EOF

${C_GREEN}OK${C_OFF} Backend prêt — frontend local à lancer séparément :

  ${C_BOLD}cd frontend && npm run dev:api${C_OFF}

  → http://localhost:5173
  → proxy /api/* vers localhost:8080 (zéro CORS)

Pour arrêter le backend : ./infra/scripts/03-stop.sh
EOF
