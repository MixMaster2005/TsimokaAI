"""Étage 2 — OCR de secours.

TODO (cœur à implémenter, hors périmètre de l'intégration initiale) :
  1. Rendu PDF -> images par page (pypdfium2 ou équivalent, ~150-200 DPI).
  2. Chargement lazy du modèle OCR en singleton (première requête qui en a besoin) :
       - candidat principal : `PaddlePaddle/PaddleOCR-VL-0.9B` (HuggingFace), via
         `transformers` AutoModel / AutoProcessor — API à confirmer dans la doc du modèle.
       - alternative CPU : `SandLogicTechnologies/DeepSeek-OCR-2-GGUF` quantifié, via
         `llama.cpp` (repo HuggingFace) — si perf CPU insuffisante.
  3. Inférence par page puis concaténation Markdown avec séparateur ``PAGE_SEPARATOR``.

Le choix de l'API d'inférence exacte (transformers vs SDK dédié) n'étant pas confirmé,
ce module reste un placeholder fonctionnel : il renvoie un résultat vide avec un warning.
"""
import logging
import threading

logger = logging.getLogger(__name__)

PAGE_SEPARATOR = "\n\n---\n\n"


class OcrFallback:
    """Singleton — le modèle n'est chargé qu'à la première conversion qui le nécessite."""

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super().__new__(cls)
                cls._instance._model = None
                cls._instance._model_lock = threading.Lock()
            return cls._instance

    def _ensure_model(self):
        if self._model is None:
            with self._model_lock:
                if self._model is None:
                    self._model = self._load_model()

    def _load_model(self):
        """TODO : chargement réel du modèle OCR (transformers AutoModel/AutoProcessor ou llama.cpp)."""
        logger.warning("Chargement du modèle OCR non implémenté (TODO) — retour placeholder")
        return None

    def convert(self, content: bytes, filename: str, pages: int = 0, warnings=None) -> dict:
        warnings = list(warnings) if warnings else []
        self._ensure_model()
        # TODO : rendu PDF -> images par page + OCR par page + concaténation PAGE_SEPARATOR.
        warnings.append("Étage 2 (OCR) non implémenté — contenu vide renvoyé (placeholder)")
        return {
            "markdown": "",
            "method": "ocr_fallback",
            "pages_processed": pages,
            "warnings": warnings,
        }
