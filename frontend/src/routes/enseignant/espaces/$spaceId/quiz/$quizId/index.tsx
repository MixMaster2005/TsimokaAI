import { useState } from 'react';
import { createFileRoute, Link, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useMembres } from '@/features/espaces/api/use-membres';
import { useQuiz } from '@/features/quiz/api/use-quiz';
import { useQuizAttemptsStats } from '@/features/quiz/api/use-quiz-attempts-stats';
import {
  useCreateAttemptCorrection,
  useCreateQuizCorrection,
  useQuizCorrections,
} from '@/features/quiz/api/use-quiz-corrections';
import { ShareQuizModal } from '@/features/quiz/components/ShareQuizModal';
import { QuizBadgeDifficulty } from '@/features/quiz/components/QuizBadgeDifficulty';
import { Badge } from '@/components/ui/badge';
import { FileQuestion } from 'lucide-react';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/quiz/$quizId/')({
  component: QuizDetailEnseignant,
});

function QuizDetailEnseignant() {
  const { spaceId, quizId } = useParams({ from: '/enseignant/espaces/$spaceId/quiz/$quizId/' });
  const { data: quiz } = useQuiz(quizId);
  const { data: stats } = useQuizAttemptsStats(quizId);
  const { data: corrections } = useQuizCorrections(quizId);
  const { data: membres } = useMembres(spaceId);

  const [commentaire, setCommentaire] = useState('');
  const [scoreCorrige, setScoreCorrige] = useState('');
  const [selectedAttemptId, setSelectedAttemptId] = useState('');

  const createQuizCorrection = useCreateQuizCorrection(quizId);
  const createAttemptCorrection = useCreateAttemptCorrection(quizId, selectedAttemptId);
  const isSubmitting = createQuizCorrection.isPending || createAttemptCorrection.isPending;

  if (!quiz) return null;

  function handleSubmitCorrection(e: React.FormEvent) {
    e.preventDefault();
    const payload = {
      ...(commentaire.trim() ? { commentaire: commentaire.trim() } : {}),
      ...(scoreCorrige !== '' ? { scoreCorrige: Number(scoreCorrige) } : {}),
    };
    const onSuccess = () => {
      setCommentaire('');
      setScoreCorrige('');
      setSelectedAttemptId('');
    };
    if (selectedAttemptId) {
      createAttemptCorrection.mutate(payload, { onSuccess });
    } else {
      createQuizCorrection.mutate(payload, { onSuccess });
    }
  }

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
            to="/enseignant/espaces/$spaceId/quiz/$quizId/take"
            params={{ spaceId, quizId }}
          >
            <Button>Prévisualiser le passage</Button>
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

      <div>
        <h3 className="mb-3 font-display text-base font-semibold text-encre">
          Tentatives des étudiants
        </h3>
        {stats && stats.length > 0 ? (
          <div className="flex flex-col gap-2">
            {stats.map((attempt) => {
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
        ) : (
          <p className="text-sm text-encre-muted">Aucune tentative pour l'instant.</p>
        )}
      </div>

      <div>
        <h3 className="mb-3 font-display text-base font-semibold text-encre">
          Corrections
        </h3>
        {corrections && corrections.length > 0 ? (
          <div className="mb-4 flex flex-col gap-2">
            {corrections.map((correction) => (
              <div
                key={correction.id}
                className="rounded-fiche border border-papier-border bg-papier-carte p-3"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-medium text-encre">
                    {correction.scoreCorrige !== null && correction.scoreCorrige !== undefined
                      ? `Score corrigé : ${correction.scoreCorrige}`
                      : 'Commentaire'}
                  </span>
                  <span className="font-mono text-[0.68rem] text-encre-muted">
                    {new Date(correction.createdAt).toLocaleDateString('fr-FR', {
                      day: 'numeric',
                      month: 'short',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                {correction.commentaire && (
                  <p className="mt-1 text-sm text-encre-muted">{correction.commentaire}</p>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="mb-4 text-sm text-encre-muted">Aucune correction pour l'instant.</p>
        )}

        <form
          onSubmit={handleSubmitCorrection}
          className="flex flex-col gap-3 rounded-fiche border border-papier-border bg-papier-carte p-4"
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="correction-attempt">Tentative concernée (optionnel)</Label>
            <select
              id="correction-attempt"
              className="rounded-md border border-papier-border bg-papier-bg px-3 py-2 text-sm text-encre"
              value={selectedAttemptId}
              onChange={(e) => setSelectedAttemptId(e.target.value)}
            >
              <option value="">Correction générale du quiz</option>
              {(stats ?? []).map((attempt) => (
                <option key={attempt.id} value={attempt.id}>
                  {attempt.score}/{attempt.totalQuestions} —{' '}
                  {new Date(attempt.attemptedAt).toLocaleDateString('fr-FR')}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="correction-commentaire">Commentaire</Label>
            <Input
              id="correction-commentaire"
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              placeholder="Votre commentaire…"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="correction-score">Score corrigé</Label>
            <Input
              id="correction-score"
              type="number"
              value={scoreCorrige}
              onChange={(e) => setScoreCorrige(e.target.value)}
              placeholder="Ex. 8"
            />
          </div>
          <div>
            <Button
              type="submit"
              disabled={isSubmitting || (!commentaire.trim() && scoreCorrige === '')}
            >
              {isSubmitting ? 'Envoi…' : 'Ajouter la correction'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
