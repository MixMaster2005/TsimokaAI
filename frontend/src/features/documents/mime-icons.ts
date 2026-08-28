import {
  FileText,
  FileSpreadsheet,
  FileCode,
  BookOpen,
  File,
  type LucideIcon,
} from 'lucide-react';

interface MimeIconInfo {
  icon: LucideIcon;
  color: string;
  label: string;
}

const MIME_MAP: Record<string, MimeIconInfo> = {
  // PDF
  'application/pdf': { icon: FileText, color: 'text-red-500', label: 'PDF' },
  // Word
  'application/msword': { icon: FileText, color: 'text-blue-600', label: 'Word' },
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': {
    icon: FileText,
    color: 'text-blue-600',
    label: 'Word',
  },
  // Excel
  'application/vnd.ms-excel': {
    icon: FileSpreadsheet,
    color: 'text-green-600',
    label: 'Excel',
  },
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': {
    icon: FileSpreadsheet,
    color: 'text-green-600',
    label: 'Excel',
  },
  // PowerPoint
  'application/vnd.ms-powerpoint': {
    icon: File,
    color: 'text-orange-500',
    label: 'PPT',
  },
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': {
    icon: File,
    color: 'text-orange-500',
    label: 'PPT',
  },
  // Texte
  'text/plain': { icon: FileText, color: 'text-muted-foreground', label: 'Texte' },
  'text/markdown': { icon: FileText, color: 'text-muted-foreground', label: 'Markdown' },
  // HTML
  'text/html': { icon: FileCode, color: 'text-purple-500', label: 'HTML' },
  'application/xhtml+xml': { icon: FileCode, color: 'text-purple-500', label: 'HTML' },
  // CSV
  'text/csv': {
    icon: FileSpreadsheet,
    color: 'text-green-600',
    label: 'CSV',
  },
  // EPUB
  'application/epub+zip': { icon: BookOpen, color: 'text-blue-500', label: 'EPUB' },
};

const EXTENSION_MAP: Record<string, MimeIconInfo> = {
  pdf: MIME_MAP['application/pdf'],
  doc: MIME_MAP['application/msword'],
  docx: MIME_MAP['application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
  xls: MIME_MAP['application/vnd.ms-excel'],
  xlsx: MIME_MAP['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
  ppt: MIME_MAP['application/vnd.ms-powerpoint'],
  pptx: MIME_MAP['application/vnd.openxmlformats-officedocument.presentationml.presentation'],
  txt: MIME_MAP['text/plain'],
  md: MIME_MAP['text/markdown'],
  csv: MIME_MAP['text/csv'],
  html: MIME_MAP['text/html'],
  htm: MIME_MAP['text/html'],
  epub: MIME_MAP['application/epub+zip'],
};

const DEFAULT_ICON: MimeIconInfo = {
  icon: File,
  color: 'text-muted-foreground',
  label: 'Fichier',
};

/**
 * Retourne l'icône, la couleur et le label associés à un type MIME.
 * Fonctionne aussi avec un nom de fichier (extrait l'extension).
 */
export function getMimeInfo(mimeType: string | null, filename?: string): MimeIconInfo {
  // 1. Essayer par MIME type exact
  if (mimeType && MIME_MAP[mimeType]) {
    return MIME_MAP[mimeType];
  }

  // 2. Essayer par extension du filename
  if (filename) {
    const ext = filename.split('.').pop()?.toLowerCase();
    if (ext && EXTENSION_MAP[ext]) {
      return EXTENSION_MAP[ext];
    }
  }

  // 3. Fallback
  return DEFAULT_ICON;
}
