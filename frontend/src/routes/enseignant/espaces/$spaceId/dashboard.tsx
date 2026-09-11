import { createFileRoute, useParams } from '@tanstack/react-router';

import { TeacherDashboardBlocks } from '@/components/shared/TeacherDashboardBlocks';
import { espaceQueryOptions } from '@/features/espaces/api/use-espace';
import { teacherDashboardQueryOptions } from '@/features/dashboard/api/use-teacher-dashboard';
import { recommandationsQueryOptions } from '@/features/dashboard/api/use-recommandations';

export const Route = createFileRoute('/enseignant/espaces/$spaceId/dashboard')({
  loader: ({ context: { queryClient }, params }) =>
    Promise.all([
      queryClient.ensureQueryData(espaceQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(teacherDashboardQueryOptions(params.spaceId)),
      queryClient.ensureQueryData(recommandationsQueryOptions(params.spaceId)),
    ]),
  component: TableauDeBordEnseignant,
});

function TableauDeBordEnseignant() {
  const { spaceId } = useParams({ from: '/enseignant/espaces/$spaceId/dashboard' });

  return (
    <div className="p-6">
      <p className="font-mono text-xs uppercase tracking-wide text-encre-muted">Espace</p>
      <h1 className="mb-6 font-display text-2xl font-semibold text-encre">Tableau de bord</h1>

      <TeacherDashboardBlocks spaceId={spaceId} />
    </div>
  );
}
