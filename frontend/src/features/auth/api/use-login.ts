import { useMutation } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';

import { apiClient, setAccessToken } from '@/lib/api-client';
import type { AuthResponse, LoginPayload } from '../types';

/**
 * Une mutation, pas une query : login n'est pas une donnée qu'on "lit" et
 * qu'on garde en cache, c'est une action. C'est la distinction TanStack
 * Query de base : GET idempotent -> useQuery, action qui change l'état
 * serveur -> useMutation.
 */
export function useLogin() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (payload: LoginPayload) =>
      apiClient.post<AuthResponse>('/api/v1/auth/login', payload),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      // TODO : persister refreshToken (httpOnly cookie côté back idéalement,
      // sinon voir la discussion OAuth précédente sur le choix de stockage)
      navigate({ to: '/' }); // -> Étagère, layout _app prend le relais
    },
  });
}
