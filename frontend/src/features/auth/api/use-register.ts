import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { setAccessToken, setRefreshToken } from '@/lib/auth-tokens';
import type { AuthResponse, RegisterPayload } from '../types';

export function useRegister() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: RegisterPayload) =>
      apiClient.post<AuthResponse>('/api/v1/auth/register', payload),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      queryClient.setQueryData(['auth', 'session'], data.user);
      navigate({ to: '/onboarding/bienvenue' });
    },
  });
}
