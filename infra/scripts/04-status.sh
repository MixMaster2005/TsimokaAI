#!/usr/bin/env bash
# ============================================================================
# État de santé de la plateforme : conteneurs + endpoint /actuator/health de
# chaque service, interrogé DANS le réseau Docker (la gateway ne route pas
# /actuator vers les services).
#
# Usage : ./infra/scripts/04-status.sh    (code retour = nb de services KO)
# ============================================================================
source "$(dirname "$0")/_lib.sh"

echo "${C_BOLD}Conteneurs${C_OFF}"
docker compose --profile ollama ps --format 'table {{.Name}}\t{{.Status}}'

echo ""
echo "${C_BOLD}Santé des services${C_OFF}"

declare -A PORTS=(
    [api-gateway]=8080 [user-service]=8081 [space-service]=8082
    [ingestion-service]=8083 [chat-service]=8084 [fiche-service]=8085
    [analytics-service]=8086 [gamification-service]=8087
)

FAILED=0
for svc in api-gateway user-service space-service ingestion-service chat-service fiche-service analytics-service gamification-service; do
    port="${PORTS[$svc]}"
    if ! container_up "tsimoka-$svc"; then
        printf '%-22s %sARRÊTÉ%s\n' "$svc" "$C_RED" "$C_OFF"
        FAILED=$((FAILED + 1))
        continue
    fi
    status="$(docker run --rm --network apa-net curlimages/curl -s --max-time 5 \
        "http://$svc:${port}/actuator/health" | grep -o '"status":"[A-Z]*"' || true)"
    case "$status" in
        *UP*)   printf '%-22s %sUP%s\n' "$svc" "$C_GREEN" "$C_OFF" ;;
        "")     printf '%-22s %ssans réponse%s\n' "$svc" "$C_YELLOW" "$C_OFF"
                FAILED=$((FAILED + 1)) ;;
        *)      printf '%-22s %s%s%s\n' "$svc" "$C_RED" "$status" "$C_OFF"
                FAILED=$((FAILED + 1)) ;;
    esac
done

echo ""
if [ "$FAILED" -eq 0 ]; then
    ok "Tous les services sont UP — http://localhost:3000"
else
    warn "$FAILED service(s) en échec — logs : ./infra/scripts/05-logs.sh <service>"
fi
exit "$FAILED"
