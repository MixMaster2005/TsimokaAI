import { queryOptions, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { recommandationsKeys } from './keys';
import type { Recommandation } from '../types';

export const teacherRecommandationsQueryOptions = (spaceId: string, studentId: string) =>
  queryOptions({
    queryKey: recommandationsKeys.teacher(spaceId, studentId),
    queryFn: () =>
      apiClient.get<Recommandation[]>(
        `/api/v1/dashboard/teacher/recommandations?spaceId=${spaceId}&studentId=${studentId}`,
      ),
    enabled: !!spaceId && !!studentId,
  });

export function useTeacherRecommandations(spaceId: string, studentId: string) {
  return useQuery(teacherRecommandationsQueryOptions(spaceId, studentId));
}
