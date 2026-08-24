/** Calqué sur ingestion-service/dto/DocumentResponse.java + entity/Document.java */

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';

export interface AppDocument {
  id: string;
  spaceId: string;
  userId: string;
  filename: string;
  mimeType: string | null;
  storageUrl: string;
  status: DocumentStatus;
  chunkCount: number | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}
