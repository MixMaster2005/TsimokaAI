#!/usr/bin/env bash
# ============================================================================
# Démarrage du backend uniquement (sans le frontend Docker) pour un workflow
# dev rapide : le frontend tourne sur l'hôte via Vite (npm run dev:api).
#
#   ./infra/scripts/08-dev-backend.sh
#
# Ollama est TOUJOURS démarré : il sert les embeddings (nomic-embed-text)
# nécessaires au RAG (chat-service, ingestion-service, fiche-service) même
# quand le LLM est Groq/Gemini.
#
# Une fois le backend up, lancer le frontend dans un autre terminal :
#   cd frontend && npm run dev:api
#
# Le proxy Vite (/api -> localhost:8080) élimine tout CORS — même comportement
# que nginx en prod.
# ============================================================================
source "$(dirname "$0")/_lib.sh"

env_file_present || fail ".env absent — lancer ./infra/scripts/01-premier-demarrage.sh d'abord."

OLLAMA_CONTAINER="tsimoka-ollama"

# --- Démarrage de la stack (ollama toujours inclus pour les embeddings) ----
info "Démarrage du backend (sans frontend Docker)"
docker compose --profile ollama up -d \
    postgres redis qdrant minio ollama \
    api-gateway user-service space-service ingestion-service \
    chat-service fiche-service analytics-service gamification-service

# --- Vérification ollama + modèle d'embedding ------------------------------
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

# Vérifier/charger le modèle d'embedding (requis pour le RAG)
if docker exec "$OLLAMA_CONTAINER" ollama list 2>/dev/null | grep -q "nomic-embed-text"; then
    ok "Modèle d'embedding nomic-embed-text déjà chargé"
else
    info "Chargement du modèle d'embedding (nomic-embed-text)..."
    docker exec "$OLLAMA_CONTAINER" ollama pull nomic-embed-text
    ok "nomic-embed-text chargé"
fi

for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    await_service "$svc"
done

"$REPO_ROOT/infra/scripts/04-status.sh"

cat <<EOF

${C_GREEN}OK${C_OFF} Backend prêt — frontend local à lancer séparément :

  ${C_BOLD}cd frontend && npm run dev:api${C_OFF}

  → http://localhost:5173
  → proxy /api/* vers localhost:8080 (zéro CORS)
  → ollama : http://localhost:11435 (nomic-embed-text chargé)

Pour arrêter le backend : ./infra/scripts/03-stop.sh
EOF
