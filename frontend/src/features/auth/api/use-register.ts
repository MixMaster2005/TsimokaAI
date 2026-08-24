import { useMutation } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient, setAccessToken } from '@/lib/api-client';
import type { AuthResponse, RegisterPayload } from '../types';

export function useRegister() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (payload: RegisterPayload) =>
      apiClient.post<AuthResponse>('/api/v1/auth/register', payload),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      navigate({ to: '/onboarding/bienvenue' });
    },
  });
}
