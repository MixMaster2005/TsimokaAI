import { useState } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { usePublishQuiz } from '@/features/quiz/api/use-publish-quiz';
import {
  quizzesEspaceQueryOptions,
  useQuizzesEspace,
} from '@/features/quiz/api/use-quizzes-espace';
import { GenerateQuizModal } from '@/features/quiz/components/GenerateQuizModal';
import { QuizCard } from '@/features/quiz/components/QuizCard';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/quiz/')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(quizzesEspaceQueryOptions(params.spaceId)),
  component: QuizEspaceEnseignant,
});

function QuizEspaceEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/quiz/' });
  const { data: quizzes } = useQuizzesEspace(spaceId);
  const publishQuiz = usePublishQuiz(spaceId);
  const [modalOpen, setModalOpen] = useState(false);

  const brouillons = (quizzes ?? []).filter((q) => q.statut === 'BROUILLON');
  const publies = (quizzes ?? []).filter((q) => q.statut !== 'BROUILLON');

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-encre">Quiz</h2>
        <Button onClick={() => setModalOpen(true)}>Générer un Quiz</Button>
      </div>

      {quizzes?.length === 0 && (
        <p className="text-sm text-encre-muted">
          Aucun quiz généré pour l'instant dans cet espace.
        </p>
      )}

      {publies.length > 0 && (
        <section className="mb-6">
          <h3 className="mb-3 font-display text-base font-semibold text-encre">
            Publiés <Badge variant="succes">{publies.length}</Badge>
          </h3>
          <div className="flex flex-col gap-3">
            {publies.map((quiz) => (
              <QuizCard key={quiz.id} quiz={quiz} spaceId={spaceId} basePath="/enseignant" />
            ))}
          </div>
        </section>
      )}

      {brouillons.length > 0 && (
        <section>
          <h3 className="mb-3 font-display text-base font-semibold text-encre">
            Brouillons <Badge variant="secondary">{brouillons.length}</Badge>
          </h3>
          <div className="flex flex-col gap-3">
            {brouillons.map((quiz) => (
              <div key={quiz.id} className="flex flex-col gap-2">
                <QuizCard quiz={quiz} spaceId={spaceId} basePath="/enseignant" />
                <div>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={publishQuiz.isPending}
                    onClick={() => publishQuiz.mutate(quiz.id)}
                  >
                    {publishQuiz.isPending ? 'Publication…' : 'Publier'}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <GenerateQuizModal open={modalOpen} onOpenChange={setModalOpen} spaceId={spaceId} />
    </div>
  );
}
