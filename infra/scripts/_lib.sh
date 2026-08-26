#!/usr/bin/env bash
# ============================================================================
# Bibliothèque partagée des scripts infra/ — à sourcer, jamais à exécuter.
#
# Conventions :
#   - tous les scripts supposent la racine du repo comme répertoire de travail
#     (déterminée ici, indépendamment de l'endroit d'où ils sont appelés) ;
#   - sortie colorée désactivée automatiquement si stdout n'est pas un TTY
#     (CI, redirection de logs).
# ============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

if [ -t 1 ]; then
    C_BOLD=$'\033[1m'; C_GREEN=$'\033[32m'; C_RED=$'\033[31m'
    C_YELLOW=$'\033[33m'; C_DIM=$'\033[2m'; C_OFF=$'\033[0m'
else
    C_BOLD=""; C_GREEN=""; C_RED=""; C_YELLOW=""; C_DIM=""; C_OFF=""
fi

info()  { printf '%s\n' "${C_DIM}==> ${C_OFF}$*"; }
ok()    { printf '%s\n' "${C_GREEN}OK ${C_OFF} $*"; }
warn()  { printf '%s\n' "${C_YELLOW}!! ${C_OFF}$*"; }
fail()  { printf '%s\n' "${C_RED}ERREUR ${C_OFF}$*" >&2; exit 1; }

# Vrai si le fichier .env existe à la racine.
env_file_present() { [ -f "$REPO_ROOT/.env" ]; }

# Vrai si le conteneur donné est en course ET sans boucle de redémarrage.
container_up() {
    local name="$1"
    [ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null)" = "true" ]
}

# Attend que le service Spring donné logge son démarrage effectif ("Started ...").
# Args : nom-de-service-compose [timeout_secondes]
await_service() {
    local svc="$1" timeout="${2:-300}" waited=0
    info "Attente du démarrage de $svc (jusqu'à ${timeout}s — les JVM sont lentes au premier boot)..."
    while ! docker logs "tsimoka-$svc" 2>&1 | grep -q "Started .*Application"; do
        if ! container_up "tsimoka-$svc"; then
            fail "$svc s'est arrêté — voir: docker logs tsimoka-$svc"
        fi
        if [ "$waited" -ge "$timeout" ]; then
            warn "Timeout en attendant $svc (${timeout}s). Il continue en arrière-plan ; vérifier avec ./infra/scripts/status.sh"
            return 1
        fi
        sleep 5; waited=$((waited + 5))
    done
    ok "$svc démarré"
}

# Point d'entrée API local (gateway), surchargable pour une stack distante.
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
