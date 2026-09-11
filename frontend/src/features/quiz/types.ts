export type QuizDifficulty = 'FACILE' | 'MOYEN' | 'DIFFICILE';
export type QuizScope = 'DOCUMENT' | 'SPACE' | 'TOPIC';
export type QuizStatut = 'BROUILLON' | 'PUBLIE';

export interface Question {
  question: string;
  type?: string;
  options?: string[];
  correct_answer: string;
  explanation: string;
  hint?: string;
}

export interface AnswerEntry {
  questionIndex: number;
  answer: string;
}

export interface Quiz {
  id: string;
  spaceId: string;
  userId: string;
  title: string | null;
  scope: QuizScope;
  targetDocumentId: string | null;
  targetTopic: string | null;
  sourceFicheId: string | null;
  sourceDocumentIds: string[];
  difficulty: QuizDifficulty;
  questionCount: number;
  contentJson: string;
  obsolete: boolean;
  statut?: QuizStatut;
  generatedAt: string;
  updatedAt: string;
}

export interface QuizAttempt {
  id: string;
  quizId: string;
  userId: string;
  answersJson: string;
  score: number;
  totalQuestions: number;
  attemptedAt: string;
}

export interface QuizShare {
  id: string;
  quizId: string;
  groupeId: string | null;
  destinataireId: string | null;
  partagePar: string;
  sharedAt: string;
}

export interface GenerateQuizRequest {
  spaceId: string;
  title?: string;
  scope: QuizScope;
  targetDocumentId?: string;
  targetTopic?: string;
  sourceFicheId?: string;
  documentIds?: string[];
  difficulty: QuizDifficulty;
  questionCount: number;
}

export interface SubmitQuizAttemptRequest {
  answersJson: string;
}

export interface ShareQuizRequest {
  groupeId?: string;
  destinataireId?: string;
}

export interface QuizCorrection {
  id: string;
  quizId: string;
  attemptId: string | null;
  enseignantId: string;
  commentaire: string | null;
  scoreCorrige: number | null;
  createdAt: string;
}

export interface CreateCorrectionRequest {
  commentaire?: string;
  scoreCorrige?: number;
}
