#!/usr/bin/env bash
# ============================================================================
# Logs d'un service (ou de tous). Suit le flux par défaut (Ctrl-C pour quitter).
#
#   ./infra/scripts/05-logs.sh chat-service          # suivre chat-service
#   ./infra/scripts/05-logs.sh chat-service --tail   # 200 dernières lignes, pas de follow
#   ./infra/scripts/05-logs.sh all                   # toute la stack (suivi)
#
# Noms acceptés : avec ou sans préfixe tsimoka- (chat-service = tsimoka-chat-service).
# ============================================================================
source "$(dirname "$0")/_lib.sh"

TARGET="${1:-all}"
MODE="${2:---follow}"

case "$MODE" in
    --follow) FOLLOW=(-f) ;;
    --tail)   FOLLOW=() ;;
    *)        fail "2e argument : --follow ou --tail" ;;
esac

case "$TARGET" in
    all) exec docker compose logs "${FOLLOW[@]}" --tail=200 ;;
    *)
        svc="${TARGET#tsimoka-}"
        docker compose ps --services | grep -qx "$svc" || fail "service inconnu : $svc"
        exec docker compose logs "${FOLLOW[@]}" --tail=200 "$svc"
        ;;
esac
