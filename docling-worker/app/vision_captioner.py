"""Vision — appel de l'API Gemini (mode OpenAI-compatible) pour décrire/transcrire des images.

Remplace l'OCR local (spec v2) : aucun modèle vision n'est embarqué dans le conteneur
(CPU-only, 8 Go RAM), on délègue à Gemini via le point d'entrée OpenAI de
Google : ``https://generativelanguage.googleapis.com/v1beta/openai/``.

Deux usages :
  1. ``caption_figure``  : légende courte d'une image extraite (figure/schéma/capture).
  2. ``transcribe_full_page`` : transcription Markdown d'une page rendue en image
     (documents scannés, là où MarkItDown n'extrait rien).

La clé API est lue dans l'environnement du conteneur (``GEMINI_API_KEY``), injectée par
``ingestion-service`` (docker-java, ``withEnv(...)``) au moment du spawn.
"""
import base64
import logging
import os
import threading
import time
from typing import Optional

from openai import OpenAI

logger = logging.getLogger(__name__)

# Point d'entrée OpenAI-compatible de Gemini (cf. https://ai.google.dev/gemini-api/docs/openai).
GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"

# Modèle vision par défaut (à ajuster si un autre nom est utilisé dans le projet).
GEMINI_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")

MAX_RETRIES = int(os.environ.get("GEMINI_MAX_RETRIES", "3"))
RETRY_BACKOFF_SECONDS = float(os.environ.get("GEMINI_RETRY_BACKOFF", "1.5"))

CAPTION_PROMPT = (
    "Rédige une légende concise en français (moins de 15 mots) décrivant cette image "
    "d'un document de cours. S'il s'agit d'un schéma ou d'un graphique, précise son sujet. "
    "Réponds uniquement par la légende, sans guillemets ni préfixe."
)

TRANSCRIBE_PROMPT = (
    "Transcris fidèlement le contenu de cette page de document en Markdown structuré "
    "(titres, listes, paragraphes). Préserve l'ordre et le sens. Ne rien inventer, "
    "n'ajoute aucun commentaire. Réponds uniquement avec la transcription."
)

IMAGE_URL_PREFIX = "data:{content_type};base64,"


class VisionCaptioner:
    """Singleton — le client OpenAI/Gemini n'est créé qu'à la première utilisation."""

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super().__new__(cls)
                cls._instance._client = None
                cls._instance._client_lock = threading.Lock()
            return cls._instance

    def _ensure_client(self) -> OpenAI:
        if self._client is None:
            with self._client_lock:
                if self._client is None:
                    api_key = os.environ.get("GEMINI_API_KEY")
                    if not api_key:
                        raise RuntimeError(
                            "GEMINI_API_KEY absente de l'environnement du conteneur "
                            "(injectée par ingestion-service via docker-java)"
                        )
                    self._client = OpenAI(api_key=api_key, base_url=GEMINI_BASE_URL)
                    logger.info("Client Gemini initialisé (modèle %s)", GEMINI_MODEL)
        return self._client

    def caption_figure(self, image: bytes, content_type: str) -> str:
        """Légende courte d'une image extraite du document."""
        return self._complete(CAPTION_PROMPT, image, content_type, max_tokens=64)

    def transcribe_full_page(self, page_image: bytes, content_type: str = "image/png") -> str:
        """Transcription Markdown d'une page rendue en image (document scanné)."""
        return self._complete(TRANSCRIBE_PROMPT, page_image, content_type, max_tokens=2048)

    def _complete(self, prompt: str, image: bytes, content_type: str, max_tokens: int) -> str:
        client = self._ensure_client()
        image_url = IMAGE_URL_PREFIX.format(content_type=content_type) + base64.b64encode(image).decode("ascii")
        last_error: Optional[Exception] = None
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                response = client.chat.completions.create(
                    model=GEMINI_MODEL,
                    messages=[
                        {
                            "role": "user",
                            "content": [
                                {"type": "text", "text": prompt},
                                {"type": "image_url", "image_url": {"url": image_url}},
                            ],
                        }
                    ],
                    max_tokens=max_tokens,
                )
                text = (response.choices[0].message.content or "").strip()
                if text:
                    return text
                last_error = RuntimeError("Gemini a renvoyé un contenu vide")
            except Exception as e:  # noqa: BLE001 - réseau/4xx/5xx : retry avec backoff
                last_error = e
                logger.warning(
                    "Appel Gemini échoué (tentative %d/%d, modèle %s) : %s",
                    attempt, MAX_RETRIES, GEMINI_MODEL, e,
                )
            if attempt < MAX_RETRIES:
                time.sleep(RETRY_BACKOFF_SECONDS * attempt)
        raise RuntimeError(f"Gemini injoignable après {MAX_RETRIES} tentatives : {last_error}")
