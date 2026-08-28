import { useQuery } from '@tanstack/react-query';

import { groupesBySpaceQueryOptions } from './keys';

export function useGroupes(spaceId: string) {
  return useQuery(groupesBySpaceQueryOptions(spaceId));
}
