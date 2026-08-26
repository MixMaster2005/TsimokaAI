#!/usr/bin/env bash
# ============================================================================
# Vérifie que la machine peut faire tourner la plateforme TsimokaAI.
# Ne modifie rien : lecture seule. À lancer en premier (ou après un souci).
#
# Usage : ./infra/scripts/prerequis.sh
# ============================================================================
source "$(dirname "$0")/_lib.sh"

FAILS=0

check() { # check <description> <commande...>
    local desc="$1"; shift
    if "$@" >/dev/null 2>&1; then
        ok "$desc"
    else
        warn "$desc — MANQUANT"
        FAILS=$((FAILS + 1))
    fi
}

echo "${C_BOLD}Prérequis TsimokaAI${C_OFF}"

check "Docker installé"                      docker --version
check "Docker Compose v2"                    docker compose version
docker info >/dev/null 2>&1 || { fail "Le daemon Docker ne répond pas — démarrer Docker puis relancer."; }

# .env : requis (JWT_SECRET au minimum), jamais committé.
if env_file_present; then
    ok ".env présent à la racine"
    for var in JWT_SECRET DB_USERNAME DB_PASSWORD GEMINI_API_KEY GROQ_API_KEY ACTIVE_LLM_PROVIDER; do
        if grep -q "^${var}=..*" .env; then
            ok "  $var renseigné"
        else
            warn "  $var absent ou vide dans .env"
            [ "$var" = "JWT_SECRET" ] && FAILS=$((FAILS + 1))
            [ "$var" = "ACTIVE_LLM_PROVIDER" ] && FAILS=$((FAILS + 1))
        fi
    done
else
    warn ".env ABSENT — copier le modèle puis le compléter :"
    echo "     cp .env.example .env"
    FAILS=$((FAILS + 1))
fi

# Ports hôtes utilisés par docker-compose.yml. Un port occupé par autre chose
# que Docker fait échouer le démarrage du conteneur concerné.
for port in 3000 5433 6333 6334 6379 8080 9000 9001 11435; do
    if ss -tln 2>/dev/null | awk '{print $4}' | grep -q ":${port}$"; then
        # Occupé : acceptable uniquement si c'est un de nos propres conteneurs.
        if docker ps --format '{{.Ports}}' | grep -q ":${port}->"; then
            ok "port ${port} occupé par un conteneur TsimokaAI"
        else
            warn "port ${port} déjà utilisé par un process non-Docker (conflit probable)"
            FAILS=$((FAILS + 1))
        fi
    else
        ok "port ${port} libre"
    fi
done

# Optionnels, avec conséquence expliquée si absents.
check "image docling-worker:latest (upload de documents)" \
    docker image inspect docling-worker:latest

if [ "$FAILS" -gt 0 ]; then
    fail "$FAILS point(s) à corriger avant ./infra/scripts/01-premier-demarrage.sh"
else
    ok "Tout est prêt — ./infra/scripts/01-premier-demarrage.sh"
fi
