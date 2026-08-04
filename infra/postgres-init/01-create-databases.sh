#!/bin/bash
# Exécuté automatiquement par l'image postgres officielle au premier démarrage
# (docker-entrypoint-initdb.d). Crée UNE base par service.
#
# Choix architectural (à justifier dans le mémoire) : une SEULE instance PostgreSQL
# héberge toutes les bases, plutôt qu'un conteneur Postgres par service. Chaque service
# reste propriétaire exclusif de SA base (aucun accès croisé, isolation logique complète
# via des identifiants/permissions distincts si besoin) — seule l'isolation physique par
# conteneur est mutualisée, pour tenir sur 8 Go de RAM sans GPU (cf. contraintes matérielles
# du mémoire). Migrer vers un Postgres par service en production ne demande qu'un changement
# de DB_URL dans chaque application.yml, la couche applicative n'a aucune dépendance à ce choix.

set -e

for db in user_db space_db ingestion_db chat_db fiche_db analytics_db gamification_db; do
  echo "Création de la base '$db' (si absente)..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

echo "Toutes les bases applicatives ont été créées."
