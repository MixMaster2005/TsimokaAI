/**
 * Calqué sur space-service/dto/SpaceResponse.java + CreateSpaceRequest.java.
 *
 * ⚠️ Note d'architecture : SpaceResponse ne contient PAS le nombre de
 * documents ni la date de dernière activité (visibles dans la maquette de
 * l'étagère) — ces infos vivent dans ingestion-service et chat-service.
 * Pour le MVP, deux options : (a) le front fait un fetch séparé par space
 * vers ces services et agrège côté client, (b) un futur BFF/agrégateur
 * expose un endpoint dédié `/spaces/enrichies`. Pas tranché — les champs
 * ci-dessous sont marqués optionnels en attendant.
 */
export interface Space {
  id: string; // UUID
  userId: string;
  name: string;
  description: string | null;
  subjectTag: string | null;
  assistantPersona: string | null;
  createdAt: string;
  updatedAt: string;

  // Non fournis par SpaceResponse actuellement — cf. note ci-dessus
  documentCount?: number;
  lastActivityAt?: string;
}

export interface CreateSpacePayload {
  name: string;
  description?: string;
  subjectTag?: string;
}

export interface UpdateSpacePayload {
  name?: string;
  description?: string;
  subjectTag?: string;
}
