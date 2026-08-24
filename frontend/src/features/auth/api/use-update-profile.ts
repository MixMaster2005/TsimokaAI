import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient, setAccessToken } from '@/lib/api-client';
import type { User } from '../types';

export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: { displayName: string }) => apiClient.patch<User>('/api/v1/users/me', payload),
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
      setAccessToken(null);
      navigate({ to: '/' });
    },
  });
}
