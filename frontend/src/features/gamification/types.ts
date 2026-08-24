/** Calqué sur gamification-service/dto/{BadgeResponse,RappelResponse,CreateRappelRequest}.java */

export interface Badge {
  id: string;
  code: string;
  nom: string;
  description: string;
  icone: string;
  obtenu: boolean;
}

export interface Rappel {
  id: string;
  spaceId: string | null;
  message: string;
  prevuLe: string;
  envoye: boolean;
}

export interface CreateRappelPayload {
  spaceId?: string;
  message: string;
  prevuLe: string;
}
