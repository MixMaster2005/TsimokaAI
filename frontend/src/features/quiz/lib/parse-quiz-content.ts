import { z } from 'zod';

import type { Question, AnswerEntry } from '../types';

/** Question brute renvoyée par le LLM (champ `answer`, `options` optionnel). */
const rawQuestionSchema = z.object({
  type: z.string().optional(),
  question: z.string(),
  options: z.array(z.string()).optional(),
  answer: z.string(),
  correct_answer: z.string().optional(),
  explanation: z.string().nullable().optional(),
  hint: z.string().optional(),
});

const quizContentSchema = z.object({
  questions: z.array(rawQuestionSchema),
});

const answerEntrySchema = z.object({
  questionIndex: z.number(),
  answer: z.string(),
});

/**
 * Parse le `contentJson` d'un quiz.
 * Filtre les questions QRC (sans options) et normalise `answer` → `correct_answer`.
 */
export function parseQuizQuestions(json: string): Question[] {
  return normalizeQuestions(json, true);
}

/**
 * Parse le `contentJson` sans filtrer les QRC (pour l'affichage des résultats).
 */
export function parseAllQuizQuestions(json: string): Question[] {
  return normalizeQuestions(json, false);
}

function normalizeQuestions(json: string, filterQRC: boolean): Question[] {
  const result = quizContentSchema.safeParse(tryParse(json));
  if (!result.success) return [];

  const questions = result.data.questions
    .filter((q) => !filterQRC || (q.options && q.options.length > 0))
    .map((q) => ({
      question: q.question,
      type: q.type,
      options: q.options,
      correct_answer: q.correct_answer ?? q.answer,
      explanation: q.explanation ?? '',
      hint: q.hint,
    }));

  return questions;
}

/**
 * Parse le `answersJson` d'une tentative.
 * Le backend renvoie un tableau brut ; l'ancien format était `{ answers: [...] }`.
 */
export function parseAttemptAnswers(json: string): AnswerEntry[] {
  const raw = tryParse(json);
  if (!raw) return [];

  const arr = Array.isArray(raw) ? raw : Array.isArray(raw?.answers) ? raw.answers : [];
  const result = z.array(answerEntrySchema).safeParse(arr);
  return result.success ? result.data : [];
}

function tryParse(json: string): unknown {
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}
