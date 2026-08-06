# docling-worker — extraction de documents (spec v2 : vision Gemini pour figures + scan).
# Conteneur spawné à la demande par ingestion-service (docker-java), PAS un service
# permanent de docker-compose.yml. À construire manuellement une fois :
#   docker build -t docling-worker:latest ./docling-worker
#
# À l'exécution, ingestion-service injecte GEMINI_API_KEY via l'env du conteneur
# (docker-java, withEnv) : le worker ne doit donc JAMAIS embarquer de secret.

FROM python:3.12-slim

# poppler-utils : utilitaires PDF (pdftotext, pdfimages…) requis par MarkItDown / rendu PDF.
RUN apt-get update \
    && apt-get install -y --no-install-recommends poppler-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

EXPOSE 8090
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
