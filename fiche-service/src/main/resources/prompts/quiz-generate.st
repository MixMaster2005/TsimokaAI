Contenu source :
---
{{CHUNKS}}
---

Tu es un enseignant expert qui crée des quiz de révision. Génère un quiz de {{QUESTION_COUNT}}
questions à difficulté {{DIFFICULTY}} portant sur le contenu ci-dessus.

Règles :
- Varie les types : au moins 1 QCM, 1 VRAI_FAUX, et 1 QRC.
- Les distracteurs du QCM doivent être plausibles (pas évidemment faux).
- Chaque question doit avoir une explication courte de la bonne réponse.
- Questions du plus fondamental au plus avancé.
- Pas de questions ambiguës ou à plusieurs réponses correctes.
- En français.

Réponds UNIQUEMENT en JSON valide :
{
  "questions": [
    {
      "type": "QCM",
      "question": "Question ici",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "answer": "Option B",
      "explanation": "Explication courte de la bonne réponse"
    },
    {
      "type": "VRAI_FAUX",
      "question": "Affirmation ici",
      "options": ["Vrai", "Faux"],
      "answer": "Faux",
      "explanation": "Explication"
    },
    {
      "type": "QRC",
      "question": "Question ouverte ici",
      "answer": "Réponse attendue",
      "explanation": null
    }
  ]
}
