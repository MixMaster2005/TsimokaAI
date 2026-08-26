#!/usr/bin/env bash
# ============================================================================
# Gestion d'UN service : démarrer, arrêter, reconstruire, logs, shell.
#
#   ./infra/scripts/06-service.sh <service> start|stop|restart|rebuild|logs|sh
#
# Exemples :
#   ./infra/scripts/06-service.sh chat-service rebuild   # recompile l'image (mvn dans Docker)
#                                                        # puis redémarre le conteneur
#   ./infra/scripts/06-service.sh space-service restart  # redémarre SANS recompilation
#   ./infra/scripts/06-service.sh fiche-service logs     # suit les logs
#
# Après un `restart`, le script attend le démarrage effectif du service Spring.
# ============================================================================
source "$(dirname "$0")/_lib.sh"

[ $# -eq 2 ] || fail "Usage: $0 <service> start|stop|restart|rebuild|logs|sh"
svc="${1#tsimoka-}"
action="$2"

case "$action" in
    start)
        docker compose up -d "$svc"
        await_service "$svc"
        ;;
    stop)
        docker compose stop "$svc"
        ;;
    restart)
        docker compose restart "$svc"
        await_service "$svc"
        ;;
    rebuild)
        # --build recompile l'image (build Maven multi-stage dans Docker) ;
        # inutile de toucher au reste de la stack.
        docker compose up -d --build "$svc"
        await_service "$svc" 600
        ;;
    logs)
        exec docker compose logs -f --tail=100 "$svc"
        ;;
    sh)
        docker exec -it "tsimoka-$svc" sh
        ;;
    *)
        fail "Action inconnue : $action"
        ;;
esac
