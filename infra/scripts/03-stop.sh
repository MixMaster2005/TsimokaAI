#!/usr/bin/env bash
# ============================================================================
# Arrêt de la plateforme. Les volumes (données PostgreSQL, Qdrant, MinIO,
# ollama) sont CONSERVÉS — un 02-start.sh redémarre tel quel.
#
#   ./infra/scripts/03-stop.sh            # arrête tout, ollama inclus s'il tourne
#   ./infra/scripts/03-stop.sh --purge    # + suppression des conteneurs/réseaux
#                                         #   (les VOLUMES restent conservés)
# ============================================================================
source "$(dirname "$0")/_lib.sh"

info "Arrêt de la stack"
docker compose --profile ollama down

if [ "${1:-}" = "--purge" ]; then
    info "Suppression des conteneurs orphelins (--purge ; les données/volumes sont conservés)"
    docker compose --profile ollama down --remove-orphans
fi

ok "Plateforme arrêtée. Données conservées dans les volumes nommés."
