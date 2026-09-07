import { Outlet, createFileRoute } from '@tanstack/react-router';

import { quizQueryOptions } from '@/features/quiz/api/use-quiz';
import { quizAttemptsQueryOptions } from '@/features/quiz/api/use-quiz-attempts';
import { espaceQueryOptions } from '@/features/espaces/api/use-espace';

export const Route = createFileRoute('/_app/espaces/$spaceId/quiz/$quizId')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(quizQueryOptions(params.quizId)),
      queryClient.ensureQueryData(quizAttemptsQueryOptions(params.quizId)),
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
    ]),
  component: QuizLayout,
});

function QuizLayout() {
  return <Outlet />;
}
