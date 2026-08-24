# ============================================================================
# TsimokaAI — Frontend (SPA React/Vite)
#
# Multi-stage :
#   1. build  : node — `npm ci` puis `vite build` (type-check strict inclus,
#               cf. script "build" de package.json : tsc -b && vite build).
#   2. runtime: nginx-alpine sert le bundle statique et proxifie /api vers
#               l'api-gateway — le navigateur ne parle qu'à la même origine,
#               donc plus aucun souci CORS en local comme en prod.
#
# VITE_API_BASE_URL est volontairement vide au moment du build : le client
# HTTP (src/lib/api-client.ts) émet alors des requêtes relatives (/api/...)
# que nginx route vers api-gateway. Le JWT reste vérifié à la gateway,
# conformément au contrat backend.
#
# Build  : docker compose build frontend
# Run    : docker compose up frontend   -> http://localhost:3000
# ============================================================================

# --- Étape 1 : build du bundle statique -------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

# Cache des dépendances : copier les manifests seuls d'abord (npm ci déterministe
# via package-lock.json), puis le reste des sources.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Chaîne vide = requêtes relatives (proxifiées par nginx). Voir api-client.ts.
ENV VITE_API_BASE_URL=""
RUN npm run build

# --- Étape 2 : service nginx -------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
