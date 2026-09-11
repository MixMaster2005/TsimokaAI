import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { clearTokens } from '@/lib/auth-tokens';

export function useDeleteAccount() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: () => apiClient.delete<void>('/api/v1/users/me'),
    onSuccess: () => {
      clearTokens();
      // Purger tout le cache React Query pour éviter les données orphelines
      queryClient.clear();
      navigate({ to: '/' });
    },
  });
}
