import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api-client';
import { espaceKeys } from './keys';

/**
 * Code d'invitation d'un espace — endpoint réservé au PROPRIÉTAIRE côté back
 * (403 sinon) : `enabled` reste false tant que le rôle n'est pas confirmé,
 * pour ne pas émettre une requête vouée à échouer.
 */
export function useInviteCode(espaceId: string, isOwner: boolean) {
  return useQuery({
    queryKey: espaceKeys.inviteCode(espaceId),
    queryFn: () =>
      apiClient.get<{ inviteCode: string }>(`/api/v1/spaces/${espaceId}/invite-code`),
    enabled: isOwner,
  });
}
