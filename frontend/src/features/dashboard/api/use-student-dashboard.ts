import { queryOptions, useQuery } from '@tanstack/react-query';

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

export const studentAllDashboardsQueryOptions = queryOptions({
  queryKey: dashboardKeys.studentAll(),
  queryFn: () => apiClient.get<StudentDashboard[]>('/api/v1/dashboard/student/all'),
});

/**
 * Taux de réussite par matière en UNE requête (Lot 3 : GET /dashboard/student/all),
 * remplace les N requêtes parallèles par espace. Le nom d'espace est résolu via
 * la liste des espaces (le dashboard transverse ne porte que les IDs).
 */
export function useTauxParEspace(spaces: Space[] | undefined) {
  const { data, ...rest } = useQuery(studentAllDashboardsQueryOptions);
  const noms = new Map((spaces ?? []).map((s) => [s.id, s.name] as const));
  const taux = (data ?? []).map((d) => ({
    spaceId: d.spaceId,
    spaceName: noms.get(d.spaceId) ?? 'Espace',
    tauxReussite: d.tauxReussite,
  }));
  return { ...rest, taux };
}
