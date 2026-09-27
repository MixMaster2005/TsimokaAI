/**
 * Calqué sur analytics-service/dto/{StudentDashboardResponse,RecommandationResponse}.java
 * (Lot 1 : les 3 types sont désormais tous générés côté back).
 *
 * Sémantique des types :
 * - CHAPITRE_DIFFICILE : questions répétées sur une même notion (>= 3).
 * - REVISION_NOTION_FAIBLE : échecs répétés aux quiz (< 50 % répété).
 * - RELANCE_INACTIVITE : aucune activité depuis plus de 7 jours (scheduler quotidien).
 */
export type TypeRecommandation = 'REVISION_NOTION_FAIBLE' | 'CHAPITRE_DIFFICILE' | 'RELANCE_INACTIVITE';

export interface Recommandation {
  id: string;
  type: TypeRecommandation;
  contenu: string;
  genereLe: string;
  lueLe: string | null;
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

export interface QuestionFrequente {
  question: string;
  nbOccurrences: number;
  dernierAsk: string;
}

export interface EvolutionSemaine {
  semaine: string;
  nbActifs: number;
}

export interface TeacherDashboard {
  spaceId: string;
  notionsLesPlusConsultees: NotionStat[];
  chapitresDifficiles: ChapitreDifficile[];
  nbEtudiantsActifs: number;
  questionsFrequentes: QuestionFrequente[];
  evolution: EvolutionSemaine[];
}

export interface StudentRow {
  userId: string;
  derniereActivite: string | null;
  nbQuestions: number;
  nbFiches: number;
  nbQuizPasses: number;
  taux: number;
}
