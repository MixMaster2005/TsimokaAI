import { useMutation } from '@tanstack/react-query';

import { apiClient, ApiError } from '@/lib/api-client';

export interface ChangePasswordPayload {
  ancienMotDePasse: string;
  nouveauMotDePasse: string;
}

/**
 * TODO (Backend) : Endpoint POST/PUT /api/v1/users/me/password non encore exposé côté user-service.
 * En attendant, ce hook prépare le contrat d'appel avec gestion gracieuse du 404/501.
 */
export function useChangePassword() {
  return useMutation({
    mutationFn: async (payload: ChangePasswordPayload) => {
      try {
        return await apiClient.put<{ success: boolean }>('/api/v1/users/me/password', payload);
      } catch (error) {
        if (error instanceof ApiError && (error.status === 404 || error.status === 501)) {
          throw new Error('Le changement de mot de passe sera bientôt disponible.');
        }
        throw error;
      }
    },
  });
}
