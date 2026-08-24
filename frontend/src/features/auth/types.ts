/**
 * Calqué EXACTEMENT sur user-service/src/main/java/.../dto/*.java et entity/User.java
 * (vérifié dans le repo au moment du scaffolding — à re-vérifier si ces DTO
 * bougent côté back, ce fichier est le seul endroit à mettre à jour côté front).
 */

export type UserRole = 'STUDENT' | 'ADMIN';

export interface User {
  id: string; // UUID
  email: string;
  displayName: string;
  role: UserRole;
  createdAt: string; // Instant sérialisé en ISO 8601
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
  role?: UserRole; // cf. le point ouvert dans la cartographie UI sur le mécanisme d'attribution du rôle
}
