import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { clearTokens } from '@/lib/auth-tokens';
import type { UpdateProfilePayload, User } from '../types';

export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) =>
      apiClient.patch<User>('/api/v1/users/me', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['auth', 'session'] });
    },
  });
}

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
