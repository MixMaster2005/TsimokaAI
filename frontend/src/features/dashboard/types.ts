/**
 * Calqué sur analytics-service/dto/{StudentDashboardResponse,RecommandationResponse}.java
 * ⚠️ Le contrat de design (page Notion) mentionne des types de recommandation
 * NOTION_A_REVOIR / CHAPITRE_A_RETRAVAILLER / CONSEIL_PERSONNALISE qui
 * n'existent PAS côté back. L'enum réel : REVISION_NOTION_FAIBLE /
 * CHAPITRE_DIFFICILE / RELANCE_INACTIVITE. À corriger dans Notion.
 */
export type TypeRecommandation = 'REVISION_NOTION_FAIBLE' | 'CHAPITRE_DIFFICILE' | 'RELANCE_INACTIVITE';

export interface Recommandation {
  id: string;
  type: TypeRecommandation;
  contenu: string;
  genereLe: string;
}

export interface StudentDashboard {
  userId: string;
  spaceId: string;
  tauxReussite: number; // 0.0 - 1.0
  notionsMaitrisees: string[];
  notionsEnCours?: string[];
  notionsFaibles: string[];
  nbQuestionsPosees: number;
  nbFichesGenerees: number;
  derniereActivite: string | null;
  recommandations: Recommandation[];
}

export interface NotionStat {
  notion: string;
  nbConsultations: number;
  nbQuestions: number;
}

export interface ChapitreDifficile {
  chapitre: string;
  scoreDifficulte: number;
}

export interface TeacherDashboard {
  spaceId: string;
  notionsLesPlusConsultees: NotionStat[];
  chapitresDifficiles: ChapitreDifficile[];
  nbEtudiantsActifs: number;
}
