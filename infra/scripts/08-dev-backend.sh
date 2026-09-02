#!/usr/bin/env bash
# ============================================================================
# Démarrage du backend uniquement (sans le frontend Docker) pour un workflow
# dev rapide : le frontend tourne sur l'hôte via Vite (npm run dev:api).
#
#   ./infra/scripts/08-dev-backend.sh                 # sans ollama
#   ./infra/scripts/08-dev-backend.sh --profile-ollama  # avec le LLM local
#
# Avec --profile-ollama, le script vérifie que le conteneur ollama est en
# route et que le modèle d'embedding nomic-embed-text est chargé (requis
# par ingestion-service). Le modèle de chat qwen2.5:3b est chargé seulement
# si ACTIVE_LLM_PROVIDER=ollama dans .env.
#
# Une fois le backend up, lancer le frontend dans un autre terminal :
#   cd frontend && npm run dev:api
#
# Le proxy Vite (/api -> localhost:8080) élimine tout CORS — même comportement
# que nginx en prod.
# ============================================================================
source "$(dirname "$0")/_lib.sh"

PROFILE_ARGS=()
USE_OLLAMA=false
[ "${1:-}" = "--profile-ollama" ] && { PROFILE_ARGS=(--profile ollama); USE_OLLAMA=true; }

env_file_present || fail ".env absent — lancer ./infra/scripts/01-premier-demarrage.sh d'abord."

# Services backend à démarrer (tout sauf frontend)
BACKEND_SERVICES=(
    postgres redis qdrant minio
    api-gateway user-service space-service ingestion-service
    chat-service fiche-service analytics-service gamification-service
)

# Ajouter ollama si demandé
if [ "$USE_OLLAMA" = true ]; then
    BACKEND_SERVICES+=(ollama)
fi

info "Démarrage du backend (sans frontend Docker)"
docker compose "${PROFILE_ARGS[@]}" up -d "${BACKEND_SERVICES[@]}"

# --- Vérification ollama + modèle d'embedding ------------------------------
if [ "$USE_OLLAMA" = true ]; then
    OLLAMA_CONTAINER="tsimoka-ollama"
    info "Attente du démarrage d'ollama..."
    waited=0
    while ! docker exec "$OLLAMA_CONTAINER" ollama list >/dev/null 2>&1; do
        if ! container_up "$OLLAMA_CONTAINER"; then
            fail "$OLLAMA_CONTAINER s'est arrêté — voir: docker logs $OLLAMA_CONTAINER"
        fi
        if [ "$waited" -ge 60 ]; then
            fail "Timeout en attendant ollama (60s)"
        fi
        sleep 3; waited=$((waited + 3))
    done
    ok "ollama démarré"

    # Vérifier/charger le modèle d'embedding (requis pour l'ingestion)
    if docker exec "$OLLAMA_CONTAINER" ollama list 2>/dev/null | grep -q "nomic-embed-text"; then
        ok "Modèle d'embedding nomic-embed-text déjà chargé"
    else
        info "Chargement du modèle d'embedding (nomic-embed-text)..."
        docker exec "$OLLAMA_CONTAINER" ollama pull nomic-embed-text
        ok "nomic-embed-text chargé"
    fi

    # Charger le modèle de chat local si le provider actif est ollama
    ACTIVE_PROVIDER="${ACTIVE_LLM_PROVIDER:-ollama}"
    if [ "$ACTIVE_PROVIDER" = "ollama" ]; then
        if docker exec "$OLLAMA_CONTAINER" ollama list 2>/dev/null | grep -q "qwen2.5:3b"; then
            ok "Modèle de chat qwen2.5:3b déjà chargé"
        else
            info "Chargement du modèle de chat (qwen2.5:3b)..."
            docker exec "$OLLAMA_CONTAINER" ollama pull qwen2.5:3b
            ok "qwen2.5:3b chargé"
        fi
    fi
fi

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

"$REPO_ROOT/infra/scripts/04-status.sh"

OLLAMA_HINT=""
if [ "$USE_OLLAMA" = true ]; then
    OLLAMA_HINT="
  Ollama : http://localhost:11435 (nomic-embed-text + qwen2.5:3b chargés)"
fi

cat <<EOF

${C_GREEN}OK${C_OFF} Backend prêt — frontend local à lancer séparément :

  ${C_BOLD}cd frontend && npm run dev:api${C_OFF}

  → http://localhost:5173
  → proxy /api/* vers localhost:8080 (zéro CORS)${OLLAMA_HINT}

Pour arrêter le backend : ./infra/scripts/03-stop.sh
EOF
