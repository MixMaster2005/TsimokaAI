import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient } from '@/lib/api-client';
import { setAccessToken, setRefreshToken } from '@/lib/auth-tokens';
import type { AuthResponse, LoginPayload } from '../types';

/**
 * Une mutation, pas une query : login n'est pas une donnée qu'on "lit" et
 * qu'on garde en cache, c'est une action. C'est la distinction TanStack
 * Query de base : GET idempotent -> useQuery, action qui change l'état
 * serveur -> useMutation.
 */
export function useLogin() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: LoginPayload) =>
      apiClient.post<AuthResponse>('/api/v1/auth/login', payload),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      queryClient.setQueryData(['auth', 'session'], data.user);
      navigate({ to: '/' });
    },
  });
}
