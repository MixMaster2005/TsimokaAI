import { useQuery } from '@tanstack/react-query';

import { objectifsBySpaceQueryOptions } from './keys';

export function useObjectifs(spaceId: string) {
  return useQuery({ ...objectifsBySpaceQueryOptions(spaceId), enabled: !!spaceId });
}
