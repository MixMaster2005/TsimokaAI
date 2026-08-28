import { useMutation } from '@tanstack/react-query';

import { apiClient, ApiError } from '@/lib/api-client';

/**
 * TODO (Backend) : Endpoint POST /api/v1/auth/forgot-password non encore implémenté côté auth/user-service.
 * En attendant, ce hook tente l'appel et gère gracieusement le 404/501.
 */
export function useForgotPassword() {
  return useMutation({
    mutationFn: async (payload: { email: string }) => {
      try {
        return await apiClient.post<{ message?: string }>('/api/v1/auth/forgot-password', payload);
      } catch (error) {
        if (error instanceof ApiError && (error.status === 404 || error.status === 501)) {
          return { message: 'Fonctionnalité bientôt disponible' };
        }
        throw error;
      }
    },
  });
}
