Résumés intermédiaires des documents sources de la fiche :
---
{{SUMMARIES}}
---

Tu es un assistant pédagogique. Fusionne ces résumés en une fiche de révision unique,
cohérente et hiérarchisée.

Règles de fusion :
1. **Élimine les redondances** : si deux documents couvrent le même concept, garde la
   version la plus complète.
2. **Hiérarchise** : la définition fondamentale d'abord, les points clés par ordre
   d'importance, l'exemple le plus parlant.
3. **Sois concis** : 3 à 7 points clés maximum. Mieux vaut 5 points essentiels que
   15 points dont la moitié est secondaire.
4. **Exemple unique** : choisis le meilleur exemple parmi les documents sources.
5. **Cohérence** : assure-toi que la fiche se lit comme un tout unifié, pas comme
   une juxtaposition de résumés.
6. **Erreurs courantes** : inclut les erreurs fréquentes ou pièges mentionnés dans les sources.
7. **Questions de révision** : génère 3 à 5 questions de révision active (Q&R) pour tester
   la compréhension des concepts clés.

Réponds uniquement en JSON valide, avec cette structure exacte :
{
  "definition": "Définition claire et complète de la notion principale",
  "key_points": ["point essentiel 1", "point essentiel 2", "..."],
  "example": "Exemple concret et appliqué",
  "common_mistakes": ["erreur fréquente 1", "erreur fréquente 2"],
  "self_quiz": [
    {"question": "Question de révision 1", "answer": "Réponse 1"},
    {"question": "Question de révision 2", "answer": "Réponse 2"}
  ]
}

Tout doit être en français. Pas de commentaires en dehors du JSON.
