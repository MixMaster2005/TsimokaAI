import { queryOptions, useQueries, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { dashboardKeys } from './keys';
import type { Space } from '@/features/espaces/types';
import type { StudentDashboard } from '../types';

export const studentDashboardQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: dashboardKeys.student(spaceId),
    queryFn: () => apiClient.get<StudentDashboard>(`/api/v1/dashboard/student?spaceId=${spaceId}`),
  });

export function useStudentDashboard(spaceId: string) {
  return useQuery({ ...studentDashboardQueryOptions(spaceId), enabled: !!spaceId });
}

/**
 * StudentDashboardResponse est scopé à UN espace (spaceId en @RequestParam
 * obligatoire) — pour afficher "taux de réussite par matière" sur TOUTES
 * les matières (cf. cartographie UI), le front doit faire un fetch par
 * espace et agréger lui-même. useQueries fait exactement ça : N requêtes
 * parallèles, un seul hook. Pas idéal niveau réseau si l'étudiant a
 * beaucoup d'espaces, mais fonctionnel pour un MVP — à remplacer par un
 * endpoint d'agrégation côté back si ça devient un problème de perf réel.
 */
export function useAllTauxReussite(spaces: Space[] | undefined) {
  return useQueries({
    queries: (spaces ?? []).map((space) => ({
      ...studentDashboardQueryOptions(space.id),
      select: (data: StudentDashboard) => ({ space, tauxReussite: data.tauxReussite }),
    })),
  });
}
