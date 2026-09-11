import { Outlet, createFileRoute } from '@tanstack/react-router';

import { quizQueryOptions } from '@/features/quiz/api/use-quiz';
import { quizAttemptsStatsQueryOptions } from '@/features/quiz/api/use-quiz-attempts-stats';
import { quizCorrectionsQueryOptions } from '@/features/quiz/api/use-quiz-corrections';
import { espaceQueryOptions } from '@/features/espaces/api/use-espace';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/quiz/$quizId')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(quizQueryOptions(params.quizId)),
      queryClient.ensureQueryData(quizAttemptsStatsQueryOptions(params.quizId)),
      queryClient.ensureQueryData(quizCorrectionsQueryOptions(params.quizId)),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]),
  component: QuizLayoutEnseignant,
});

function QuizLayoutEnseignant() {
  return <Outlet />;
}
