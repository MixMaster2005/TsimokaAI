#!/usr/bin/env bash
# ============================================================================
# Test de bout en bout de la plateforme, via la gateway uniquement (comme le
# ferait le frontend). Non destructif : utilisateurs de test suffixés par un
# timestamp, donc ré-exécutable sans collision.
#
# Parcours vérifié :
#   1. Inscription + login (user-service via gateway)
#   2. Création d'espace — le persona est généré par le LLM actif (space-service)
#   3. Lecture du code d'invitation (propriétaire uniquement)
#   4. Second utilisateur : rejoindre l'espace via le code (space-service)
#   5. Liste des membres + "mes espaces" du membre (owner=false attendu)
#   6. Conversation + message : réponse RAG du LLM actif (chat-service)
#   7. Vue transverse "mes fiches" (fiche-service)
#
# Hors périmètre (nécessitent docling-worker + un vrai document) :
#   upload/ingestion, citations nommées, génération de fiche.
#
# Usage : ./infra/scripts/07-test-e2e.sh [GATEWAY_URL]
# ============================================================================
source "$(dirname "$0")/_lib.sh"

GATEWAY_URL="${1:-http://localhost:8080}"
STAMP="$(date +%s)"
PASS=0; FAIL=0

step() { printf '\n%s%s%s\n' "$C_BOLD" "$1" "$C_OFF"; }
check_ok()   { ok   "$*"; PASS=$((PASS+1)); }
check_fail() { fail "$*"; } # fail sort avec code 1

# api <method> <path> <token|-> <json-body|->
api() {
    local method="$1" path="$2" token="$3" body="$4"
    local args=(-s --max-time 120 -X "$method" "$GATEWAY_URL$path"
                -H 'Content-Type: application/json')
    [ "$token" != "-" ] && args+=(-H "Authorization: Bearer $token")
    [ "$body" != "-" ] && args+=(-d "$body")
    curl "${args[@]}"
}

# jsonget <json> <chemin python sur l'objet data> — échoue si success=false
jsondata() {
    python3 -c "
import sys, json
d = json.load(sys.stdin)
assert d.get('success'), d.get('error')
print(json.dumps(d['data']) if isinstance(d['data'], (dict, list)) else d['data'])
${1:-}"
}

step "1/7 Auth : inscription + login"
EMAIL="e2e-${STAMP}@test.local"
RESP=$(api POST /api/v1/auth/register - "{\"email\":\"${EMAIL}\",\"password\":\"motdepasse123\",\"displayName\":\"E2E Test\"}")
TOKEN=$(echo "$RESP" | jsondata | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])") \
    && check_ok "utilisateur $EMAIL inscrit" || check_fail "inscription impossible"

TOKEN=$(api POST /api/v1/auth/login - "{\"email\":\"${EMAIL}\",\"password\":\"motdepasse123\"}" \
        | jsondata | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

step "2/7 Espace : création (persona LLM)"
SPACE=$(api POST /api/v1/spaces "$TOKEN" '{"name":"E2E Algebre","subjectTag":"sciences"}' \
        | jsondata)
SPACE_ID=$(echo "$SPACE" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
PERSONA=$(echo "$SPACE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['assistantPersona'] or ''))")
[ "${PERSONA:-0}" -gt 20 ] && check_ok "espace créé ($SPACE_ID), persona généré (${PERSONA} caractères)" \
                          || check_fail "persona vide ou trop court"

step "3/7 Invitation : lecture du code (propriétaire)"
CODE=$(api GET "/api/v1/spaces/$SPACE_ID/invite-code" "$TOKEN" - | jsondata \
       | python3 -c "import sys,json; print(json.load(sys.stdin)['inviteCode'])")
[ -n "$CODE" ] && check_ok "code d'invitation : $CODE" || check_fail "code illisible"

step "4/7 Adhésion : second utilisateur via le code"
EMAIL2="e2e2-${STAMP}@test.local"
api POST /api/v1/auth/register - "{\"email\":\"${EMAIL2}\",\"password\":\"motdepasse123\",\"displayName\":\"E2E Deux\"}" >/dev/null
TOKEN2=$(api POST /api/v1/auth/login - "{\"email\":\"${EMAIL2}\",\"password\":\"motdepasse123\"}" \
         | jsondata | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
OWNER=$(api POST /api/v1/spaces/join "$TOKEN2" "{\"code\":\"$CODE\"}" | jsondata \
        | python3 -c "import sys,json; print(json.load(sys.stdin)['owner'])")
[ "$OWNER" = "False" ] && check_ok "adhésion réussie (owner=false côté membre)" || check_fail "owner inattendu : ${OWNER:-?}"

step "5/7 Membres et espaces partagés"
N=$(api GET "/api/v1/spaces/$SPACE_ID/membres" "$TOKEN" - | jsondata | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
[ "$N" = "1" ] && check_ok "1 membre listé" || check_fail "membres attendus : 1, obtenu : ${N:-?}"
MINE=$(api GET /api/v1/spaces "$TOKEN2" - | jsondata | python3 -c "
import sys, json
spaces = json.load(sys.stdin)
print(any(not s.get('owner') and s['id'] == '$SPACE_ID' for s in spaces))")
[ "$MINE" = "True" ] && check_ok "l'espace apparaît chez le membre (non-propriétaire)" || check_fail "espace absent des espaces du membre"

step "6/7 Chat RAG : conversation + message (LLM actif)"
CONV_ID=$(api POST /api/v1/conversations "$TOKEN2" "{\"spaceId\":\"$SPACE_ID\",\"title\":\"E2E\"}" \
          | jsondata | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
REPLY=$(api POST "/api/v1/conversations/$CONV_ID/messages" "$TOKEN2" '{"content":"Bonjour, présente-toi en une phrase."}' \
        | jsondata | python3 -c "import sys,json; m=json.load(sys.stdin); print(m['modelUsed'], '|', len(m['content']))")
[ -n "$REPLY" ] && check_ok "réponse assistant reçue ($REPLY)" || check_fail "pas de réponse du chat (LLM indisponible ? circuit breaker ?)"

step "7/7 Fiches : vue transverse"
COUNT=$(api GET /api/v1/fiches/mine "$TOKEN2" - | jsondata | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
[ -n "$COUNT" ] && check_ok "GET /fiches/mine OK ($COUNT fiche(s))" || check_fail "endpoint fiches/mine en erreur"

echo ""
echo "${C_BOLD}Résultat : $PASS étapes OK${C_OFF}"
[ "$FAIL" -eq 0 ]
