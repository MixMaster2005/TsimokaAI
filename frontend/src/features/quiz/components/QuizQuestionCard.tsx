import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { cn } from '@/lib/utils';
import type { Question } from '../types';

interface QuizQuestionCardProps {
  question: Question;
  questionIndex: number;
  selectedAnswer: string | null;
  onSelect: (answer: string) => void;
  showFeedback?: boolean;
}

export function QuizQuestionCard({
  question,
  questionIndex,
  selectedAnswer,
  onSelect,
  showFeedback,
}: QuizQuestionCardProps) {
  return (
    <div className="rounded-fiche border border-papier-border bg-papier-carte p-5">
      <p className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">
        Question {questionIndex + 1}
      </p>
      <h4 className="mb-4 font-display text-lg font-semibold text-encre">{question.question}</h4>

      <RadioGroup value={selectedAnswer ?? undefined} onValueChange={onSelect} className="grid gap-2">
        {(question.options ?? []).map((option) => {
          const isCorrect = option === question.correct_answer;
          const isSelected = option === selectedAnswer;

          return (
            <label
              key={option}
              className={cn(
                'flex cursor-pointer items-center gap-3 rounded-fiche border px-3 py-2.5 text-sm transition-colors',
                !showFeedback && 'hover:bg-secondary has-checked:border-border has-checked:bg-secondary',
                showFeedback && isCorrect && 'border-succes bg-green-50 text-encre',
                showFeedback && isSelected && !isCorrect && 'border-erreur bg-red-50 text-encre',
                showFeedback && !isSelected && !isCorrect && 'border-papier-border bg-papier-carte text-encre-muted opacity-60',
              )}
            >
              <RadioGroupItem
                value={option}
                disabled={showFeedback}
                className={cn(
                  showFeedback && isCorrect && 'border-succes text-succes',
                  showFeedback && isSelected && !isCorrect && 'border-erreur text-erreur',
                )}
              />
              <span className="flex-1">{option}</span>
            </label>
          );
        })}
      </RadioGroup>
    </div>
  );
}
