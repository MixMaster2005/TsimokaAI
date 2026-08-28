import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { dashboardKeys } from './keys';
import type { TeacherDashboard } from '../types';

export const teacherDashboardQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: dashboardKeys.teacher(spaceId),
    queryFn: () => apiClient.get<TeacherDashboard>(`/api/v1/dashboard/teacher?spaceId=${spaceId}`),
  });

export function useTeacherDashboard(spaceId: string) {
  return useQuery(teacherDashboardQueryOptions(spaceId));
}
