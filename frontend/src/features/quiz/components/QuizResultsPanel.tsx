import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import type { Quiz, QuizAttempt } from '../types';
import { parseAllQuizQuestions, parseAttemptAnswers } from '../lib/parse-quiz-content';
import { QuizQuestionFeedback } from './QuizQuestionFeedback';

interface QuizResultsPanelProps {
  attempt: QuizAttempt;
  quiz: Quiz;
  onRetry?: () => void;
}

export function QuizResultsPanel({ attempt, quiz, onRetry }: QuizResultsPanelProps) {
  const answers = parseAttemptAnswers(attempt.answersJson);
  const questions = parseAllQuizQuestions(quiz.contentJson);

  return (
    <div className="flex flex-col gap-6">
      <div className="rounded-fiche border border-papier-border bg-papier-carte p-6 text-center">
        <p className="font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">Score</p>
        <p className="mt-1 font-display text-4xl font-semibold text-encre">
          {attempt.score}/{attempt.totalQuestions}
        </p>
        <p className="mt-1 text-sm text-encre-muted">
          {attempt.totalQuestions > 0 ? Math.round((attempt.score / attempt.totalQuestions) * 100) : 0}%
        </p>
        <Progress value={attempt.totalQuestions > 0 ? (attempt.score / attempt.totalQuestions) * 100 : 0} className="mt-4" />
      </div>

      <div className="flex flex-col gap-4">
        {answers.map((answer) => {
          const question = questions[answer.questionIndex];
          if (!question) return null;
          const isCorrect = answer.answer === question.correct_answer;
          return (
            <QuizQuestionFeedback
              key={answer.questionIndex}
              question={question}
              selectedAnswer={answer.answer}
              isCorrect={isCorrect}
            />
          );
        })}
      </div>

      {onRetry && (
        <div className="flex justify-center">
          <Button variant="outline" onClick={onRetry}>
            Réessayer
          </Button>
        </div>
      )}
    </div>
  );
}
