import { useState } from 'react';
import { createFileRoute, useParams } from '@tanstack/react-router';

import { Button } from '@/components/ui/button';
import { quizzesBySpaceQueryOptions, useQuizzes } from '@/features/quiz/api/use-quizzes';
import { GenerateQuizModal } from '@/features/quiz/components/GenerateQuizModal';
import { QuizCard } from '@/features/quiz/components/QuizCard';

export const Route = createFileRoute('/_app/espaces/$spaceId/quiz/')({
  loader: ({ context: { queryClient }, params }) =>
    queryClient.ensureQueryData(quizzesBySpaceQueryOptions(params.spaceId)),
  component: QuizEspace,
});

function QuizEspace() {
  const { spaceId } = useParams({ from: '/_app/espaces/$spaceId/quiz/' });
  const { data: quizzes } = useQuizzes(spaceId);
  const [modalOpen, setModalOpen] = useState(false);

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

      <div className="flex flex-col gap-3">
        {quizzes?.map((quiz) => (
          <QuizCard key={quiz.id} quiz={quiz} spaceId={spaceId} basePath="/espaces/$spaceId" />
        ))}
      </div>

      <GenerateQuizModal open={modalOpen} onOpenChange={setModalOpen} spaceId={spaceId} />
    </div>
  );
}
