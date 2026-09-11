import { useState, useMemo, useCallback } from 'react';
import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { quizQueryOptions, useQuiz } from '@/features/quiz/api/use-quiz';
import { useSubmitQuiz } from '@/features/quiz/api/use-submit-quiz';
import { QuizQuestionCard } from '@/features/quiz/components/QuizQuestionCard';
import { QuizResultsPanel } from '@/features/quiz/components/QuizResultsPanel';
import { parseQuizQuestions } from '@/features/quiz/lib/parse-quiz-content';
import type { Question } from '@/features/quiz/types';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/quiz/$quizId/take')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(quizQueryOptions(params.quizId)),
  component: QuizTakeEnseignant,
});

function QuizTakeEnseignant() {
  const { spaceId, quizId } = useParams({ from: '/enseignant/espaces/$spaceId/quiz/$quizId/take' });
  const { data: quiz } = useQuiz(quizId);
  const submitQuiz = useSubmitQuiz(quizId);

  const questions: Question[] = useMemo(() => {
    if (!quiz) return [];
    return parseQuizQuestions(quiz.contentJson);
  }, [quiz]);

  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Map<number, string>>(new Map());
  const [submitted, setSubmitted] = useState(false);

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
      answersArray.push({ questionIndex: key, answer: value });
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

  if (!quiz || questions.length === 0) return null;

  if (submitted && submitQuiz.data) {
    return (
      <div className="mx-auto max-w-2xl p-6">
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
    <div className="mx-auto max-w-2xl p-6">
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
