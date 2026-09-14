Contenu source :
---
{{CHUNKS}}
---

Tu es un enseignant expert qui crée des quiz de révision. Génère un quiz de {{QUESTION_COUNT}}
questions à difficulté {{DIFFICULTY}} portant sur le contenu ci-dessus.

Thème imposé : {{TOPIC}} — questions centrées exclusivement sur ce thème.

Règles :
- Si un thème imposé est indiqué ci-dessus (non vide), chaque question porte exclusivement sur ce thème ;
  ignore tout contenu hors thème.
- Chaque question doit être un QCM (4 options) ou un VRAI_FAUX (2 options).
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
    }
  ]
}
