import { CheckCircle, XCircle } from 'lucide-react';

import { cn } from '@/lib/utils';
import type { Question } from '../types';

interface QuizQuestionFeedbackProps {
  question: Question;
  selectedAnswer: string;
  isCorrect: boolean;
}

export function QuizQuestionFeedback({ question, selectedAnswer, isCorrect }: QuizQuestionFeedbackProps) {
  return (
    <div
      className={cn(
        'rounded-fiche border p-4',
        isCorrect ? 'border-succes bg-green-50' : 'border-erreur bg-red-50',
      )}
    >
      <p className="mb-2 text-sm font-medium text-encre">{question.question}</p>

      <div className="mb-2 flex items-center gap-2">
        {isCorrect ? (
          <CheckCircle className="size-5 text-succes" />
        ) : (
          <XCircle className="size-5 text-erreur" />
        )}
        <span className="font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">
          {isCorrect ? 'Correct' : 'Incorrect'}
        </span>
      </div>

      <p className="text-sm text-encre">
        <span className="font-medium">Votre réponse :</span> {selectedAnswer}
      </p>

      {!isCorrect && (
        <p className="mt-1 text-sm text-encre">
          <span className="font-medium">Bonne réponse :</span> {question.correct_answer}
        </p>
      )}

      <p className="mt-2 text-sm leading-relaxed text-encre-muted">{question.explanation}</p>
    </div>
  );
}
