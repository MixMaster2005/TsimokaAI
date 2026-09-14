import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { dashboardKeys } from './keys';
import type { StudentRow } from '../types';

export const teacherStudentsQueryOptions = (spaceId: string) =>
  queryOptions({
    queryKey: [...dashboardKeys.teacher(spaceId), 'students'] as const,
    queryFn: () => apiClient.get<StudentRow[]>(`/api/v1/dashboard/teacher/students?spaceId=${spaceId}`),
    enabled: !!spaceId,
  });

export function useTeacherStudents(spaceId: string) {
  return useQuery(teacherStudentsQueryOptions(spaceId));
}
