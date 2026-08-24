/**
 * Calqué sur gamification-service/dto/{ObjectifResponse,CreateObjectifRequest}.java
 * ⚠️ La cartographie UI (page Notion) mentionne un statut EXPIRE qui n'existe
 * PAS côté back — l'enum réel est EN_COURS / ATTEINT / ABANDONNE. À corriger
 * dans la page Notion, ce fichier fait foi entre les deux en attendant.
 */
export type StatutObjectif = 'EN_COURS' | 'ATTEINT' | 'ABANDONNE';

export interface Objectif {
  id: string;
  spaceId: string;
  titre: string;
  description: string | null;
  dateEcheance: string | null; // LocalDate ISO (YYYY-MM-DD)
  statut: StatutObjectif;
  createdAt: string;
}

export interface CreateObjectifPayload {
  spaceId: string;
  titre: string;
  description?: string;
  dateEcheance?: string;
}
