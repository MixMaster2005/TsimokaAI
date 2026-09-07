import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { useMembres } from '@/features/espaces/api/use-membres';
import { useQuiz } from '@/features/quiz/api/use-quiz';
import { useQuizAttempts } from '@/features/quiz/api/use-quiz-attempts';
import { ShareQuizModal } from '@/features/quiz/components/ShareQuizModal';
import { QuizBadgeDifficulty } from '@/features/quiz/components/QuizBadgeDifficulty';
import { Badge } from '@/components/ui/badge';
import { FileQuestion } from 'lucide-react';

export const Route = createFileRoute('/_app/espaces/$spaceId/quiz/$quizId/')({
  component: QuizDetail,
});

function QuizDetail() {
  const { spaceId, quizId } = useParams({ from: '/_app/espaces/$spaceId/quiz/$quizId/' });
  const { data: quiz } = useQuiz(quizId);
  const { data: attempts } = useQuizAttempts(quizId);
  const { data: membres } = useMembres(spaceId);

  if (!quiz) return null;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-6">
      <div className="rounded-fiche border border-papier-border bg-papier-carte p-5">
        <div className="mb-1 font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">
          Quiz
        </div>
        <div className="flex items-start justify-between">
          <div className="min-w-0 flex-1">
            <h2 className="font-display text-xl font-semibold text-encre">{quiz.title ?? 'Quiz sans titre'}</h2>
          </div>
          <QuizBadgeDifficulty difficulty={quiz.difficulty} />
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-2 text-sm text-encre-muted">
          <Badge variant="secondary">
            <FileQuestion className="mr-1 size-3" />
            {quiz.questionCount} question{quiz.questionCount > 1 ? 's' : ''}
          </Badge>
          <span>Généré le {new Date(quiz.generatedAt).toLocaleDateString('fr-FR')}</span>
        </div>

        <div className="mt-4 flex gap-2 border-t border-dashed border-papier-border pt-4">
          <Link
            to="/espaces/$spaceId/quiz/$quizId/take"
            params={{ spaceId, quizId }}
          >
            <Button>Passer le quiz</Button>
          </Link>
          <ShareQuizModal
            quizId={quizId}
            membres={(membres ?? []).map((m) => ({
              id: m.userId,
              label: `Membre ${m.userId.slice(0, 8)}…`,
            }))}
            trigger={<Button variant="outline">Partager</Button>}
          />
        </div>
      </div>

      {attempts && attempts.length > 0 && (
        <div>
          <h3 className="mb-3 font-display text-base font-semibold text-encre">
            Tentatives
          </h3>
          <div className="flex flex-col gap-2">
            {attempts.map((attempt) => {
              const pct = attempt.totalQuestions > 0 ? Math.round((attempt.score / attempt.totalQuestions) * 100) : 0;
              return (
                <div
                  key={attempt.id}
                  className="flex items-center justify-between rounded-fiche border border-papier-border bg-papier-carte p-3"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium text-encre">
                      {attempt.score}/{attempt.totalQuestions}
                    </span>
                    <Badge
                      variant={
                        pct >= 80
                          ? 'succes'
                          : pct >= 50
                            ? 'default'
                            : 'attention'
                      }
                    >
                      {pct}%
                    </Badge>
                  </div>
                  <span className="font-mono text-[0.68rem] text-encre-muted">
                    {new Date(attempt.attemptedAt).toLocaleDateString('fr-FR', {
                      day: 'numeric',
                      month: 'short',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
