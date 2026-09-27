import { useState, useMemo, useCallback } from 'react';
import { createFileRoute, Link, useBlocker, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { quizQueryOptions, useQuiz } from '@/features/quiz/api/use-quiz';
import { useSubmitQuiz } from '@/features/quiz/api/use-submit-quiz';
import { QuizQuestionCard } from '@/features/quiz/components/QuizQuestionCard';
import { QuizResultsPanel } from '@/features/quiz/components/QuizResultsPanel';
import { parseQuizQuestionsWithRawIndex } from '@/features/quiz/lib/parse-quiz-content';
import type { Question } from '@/features/quiz/types';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/quiz/$quizId/take')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(quizQueryOptions(params.quizId)),
  component: QuizTakeEnseignant,
});

function QuizTakeEnseignant() {
  const { spaceId, quizId } = useParams({ from: '/enseignant/espaces/$spaceId/quiz/$quizId/take' });
  const { data: quiz, isLoading } = useQuiz(quizId);
  const submitQuiz = useSubmitQuiz(quizId);

  // Passage filtré (QCM uniquement) + index brut pour la soumission :
  // le scoring backend lit le tableau non filtré, les `questionIndex`
  // envoyés doivent donc être des indices bruts (cf. P1-1).
  const questionsWithIndex = useMemo(() => {
    if (!quiz) return [];
    return parseQuizQuestionsWithRawIndex(quiz.contentJson);
  }, [quiz]);

  const questions: Question[] = useMemo(
    () => questionsWithIndex.map((e) => e.question),
    [questionsWithIndex],
  );

  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Map<number, string>>(new Map());
  const [submitted, setSubmitted] = useState(false);

  // Garde anti-perte (prévisualisation enseignant) : mêmes règles que côté étudiant.
  const { proceed, reset, status } = useBlocker({
    shouldBlockFn: () => answers.size > 0 && !submitted,
    enableBeforeUnload: true,
    withResolver: true,
  });

  const totalQuestions = questions.length;
  const progress = totalQuestions > 0 ? ((currentStep + 1) / totalQuestions) * 100 : 0;
  const isLastStep = currentStep === totalQuestions - 1;
  const allAnswered = answers.size === totalQuestions;

  const selectAnswer = useCallback((answer: string) => {
    setAnswers((prev) => {
      const next = new Map(prev);
      next.set(currentStep, answer);
      return next;
    });
  }, [currentStep]);

  function handleSubmit() {
    if (!allAnswered) return;
    const answersArray: { questionIndex: number; answer: string }[] = [];
    answers.forEach((value, key) => {
      answersArray.push({ questionIndex: questionsWithIndex[key]?.rawIndex ?? key, answer: value });
    });
    submitQuiz.mutate(
      { answersJson: JSON.stringify(answersArray) },
      { onSuccess: () => setSubmitted(true) },
    );
  }

  function handleRetry() {
    setSubmitted(false);
    setCurrentStep(0);
    setAnswers(new Map());
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl p-4 sm:p-6">
        <div role="status" aria-live="polite" aria-busy="true" className="flex flex-col gap-3">
          <Skeleton className="h-6 w-2/3" />
          <Skeleton className="h-40 w-full" />
          <span className="sr-only">Chargement du quiz…</span>
        </div>
      </div>
    );
  }

  if (!quiz || questions.length === 0) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-start gap-3 p-4 sm:p-6">
        <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Quiz</p>
        <h1 className="font-display text-xl font-semibold text-encre">Aucune question</h1>
        <p className="text-sm text-encre-muted">
          Ce quiz ne contient pas encore de question. Retourne à la fiche ou régénère le quiz.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link to="/enseignant/espaces/$spaceId/quiz/$quizId" params={{ spaceId, quizId }}>
            <Button variant="outline">Retour au quiz</Button>
          </Link>
          <Link to="/enseignant/espaces/$spaceId/fiches" params={{ spaceId }}>
            <Button>Retour aux fiches</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (submitted && submitQuiz.data) {
    return (
      <div className="mx-auto max-w-2xl p-4 sm:p-6">
        <div className="mb-4 rounded-fiche border border-dashed border-papier-border bg-papier-carte p-3 text-sm text-encre-muted">
          Prévisualisation enseignant — vos tentatives de test
        </div>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold text-encre">Résultats</h2>
          <Link
            to="/enseignant/espaces/$spaceId/quiz/$quizId"
            params={{ spaceId, quizId }}
          >
            <Button variant="ghost" size="sm">Retour au quiz</Button>
          </Link>
        </div>
        <QuizResultsPanel attempt={submitQuiz.data} quiz={quiz} onRetry={handleRetry} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl p-4 sm:p-6">
      <Dialog open={status === 'blocked'} onOpenChange={(ouvert) => { if (!ouvert) reset?.(); }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Quitter sans enregistrer ?</DialogTitle>
            <DialogDescription>
              Tes réponses de test ne sont pas soumises. Si tu quittes maintenant, elles seront perdues.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" size="sm" onClick={() => reset?.()} autoFocus>
              Rester
            </Button>
            <Button variant="destructive" size="sm" onClick={() => proceed?.()}>
              Quitter quand même
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <div className="mb-4 rounded-fiche border border-dashed border-papier-border bg-papier-carte p-3 text-sm text-encre-muted">
        Prévisualisation enseignant — vos tentatives de test
      </div>
      <div className="mb-6">
        <div className="mb-2 flex items-center justify-between text-sm text-encre-muted">
          <span>
            Question {currentStep + 1} / {totalQuestions}
          </span>
          <span>{Math.round(progress)}%</span>
        </div>
        <Progress value={progress} />
      </div>

      <QuizQuestionCard
        question={questions[currentStep]}
        questionIndex={currentStep}
        selectedAnswer={answers.get(currentStep) ?? null}
        onSelect={selectAnswer}
      />

      <div className="mt-6 flex justify-between">
        <Button
          variant="ghost"
          onClick={() => setCurrentStep((s) => s - 1)}
          disabled={currentStep === 0}
        >
          Précédent
        </Button>

        {isLastStep ? (
          <Button onClick={handleSubmit} disabled={!allAnswered || submitQuiz.isPending}>
            {submitQuiz.isPending ? 'Soumission…' : 'Soumettre'}
          </Button>
        ) : (
          <Button
            onClick={() => setCurrentStep((s) => s + 1)}
            disabled={!answers.has(currentStep)}
          >
            Suivant
          </Button>
        )}
      </div>
    </div>
  );
}
