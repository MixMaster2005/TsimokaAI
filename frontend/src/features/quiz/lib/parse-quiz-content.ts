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
  return parseQuizQuestionsWithRawIndex(json).map((e) => e.question);
}

/**
 * Variante de `parseQuizQuestions` qui conserve l'index brut de chaque
 * question dans `contentJson.questions[]`.
 *
 * Le scoring backend (`QuizGenerationService.scoreAttempt`) lit le tableau
 * brut non filtré : les `questionIndex` soumis doivent donc être des indices
 * bruts, même si le passage n'affiche que le sous-ensemble filtré (QCM).
 */
export function parseQuizQuestionsWithRawIndex(json: string): { question: Question; rawIndex: number }[] {
  const result = quizContentSchema.safeParse(tryParse(json));
  if (!result.success) return [];

  const out: { question: Question; rawIndex: number }[] = [];
  result.data.questions.forEach((q, rawIndex) => {
    if (!(q.options && q.options.length > 0)) return;
    out.push({
      question: {
        question: q.question,
        type: q.type,
        options: q.options,
        correct_answer: q.correct_answer ?? q.answer,
        explanation: q.explanation ?? '',
        hint: q.hint,
      },
      rawIndex,
    });
  });

  return out;
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
  if (!raw || typeof raw !== 'object') return [];

  if (Array.isArray(raw)) {
    const result = z.array(answerEntrySchema).safeParse(raw);
    return result.success ? result.data : [];
  }

  const wrapper = raw as { answers?: unknown };
  const arr = Array.isArray(wrapper.answers) ? wrapper.answers : [];
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
