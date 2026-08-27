/**
 * Gestion des tokens d'authentification.
 *
 * - accessToken : en mémoire (variable module), courte durée (~15 min).
 *   Se vide au rechargement de page — c'est le refresh token qui permet
 *   de retrouver un état authentifié silencieusement via l'interceptor 401.
 * - refreshToken : dans localStorage, durée 30 jours, rotation à chaque
 *   usage. Hashé SHA-256 côté back (inutilisable seul).
 */

const REFRESH_TOKEN_KEY = `${import.meta.env.REFRESH_TOKEN_KEY ?? 'tsimoka_refresh_token'}`;

let accessToken: string | null = null;

// --- Access token (mémoire) ---

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

// --- Refresh token (localStorage) ---

export function getRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setRefreshToken(token: string | null) {
  try {
    if (token) {
      localStorage.setItem(REFRESH_TOKEN_KEY, token);
    } else {
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  } catch {
    // localStorage indisponible (mode privé, quota…)
  }
}

// --- Nettoyage combiné ---

export function clearTokens() {
  accessToken = null;
  setRefreshToken(null);
}
