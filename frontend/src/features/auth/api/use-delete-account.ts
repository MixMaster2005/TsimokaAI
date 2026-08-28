import { useMutation } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { clearTokens } from '@/lib/auth-tokens';

export function useDeleteAccount() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: () => apiClient.delete<void>('/api/v1/users/me'),
    onSuccess: () => {
      clearTokens();
      navigate({ to: '/' });
    },
  });
}
