/**
 * Calqué sur space-service/dto/{GroupeResponse,MembreGroupeResponse}.java.
 * ⚠️ Piège : contrairement à Space (`name`), Groupe utilise `nom` — DTO
 * pas homogène côté back entre les deux entités. Pas une erreur de copie
 * ici, c'est vraiment `nom` sur Groupe.
 */
export type RoleGroupe = 'MEMBRE' | 'ANIMATEUR';

export interface Groupe {
  id: string;
  spaceId: string;
  nom: string;
  description: string | null;
  createdBy: string;
  createdAt: string;
}

export interface MembreGroupe {
  id: string;
  groupeId: string;
  userId: string;
  roleGroupe: RoleGroupe;
  joinedAt: string;
}

export interface CreateGroupePayload {
  nom: string;
  description?: string;
}
