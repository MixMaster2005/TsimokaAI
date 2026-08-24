/**
 * Calqué sur fiche-service/dto/FicheResponse.java + service/FicheContent.java
 * (contenu JSONB, vérifié dans le repo — clés en snake_case côté back
 * via @JsonProperty, donc PAS de camelCase automatique ici).
 */
export interface Fiche {
  id: string;
  spaceId: string;
  userId: string;
  title: string;
  sourceDocumentIds: string[];
  contentJson: string; // JSON.stringify(FicheContent) côté back — à parser, voir parseFicheContent()
  obsolete: boolean;
  generatedAt: string;
  updatedAt: string;
}

/** Le contenu structuré une fois `contentJson` parsé. Clés en snake_case (cf. @JsonProperty du back). */
export interface FicheContent {
  definition: string;
  key_points: string[];
  example: string;
}

export function parseFicheContent(fiche: Fiche): FicheContent | null {
  try {
    return JSON.parse(fiche.contentJson) as FicheContent;
  } catch {
    return null;
  }
}

export interface GenerateFichePayload {
  spaceId: string;
  title?: string;
  documentIds?: string[]; // vide/absent = tout le corpus de l'espace
}
