import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Fusionne des classes Tailwind en résolvant les conflits (ex: "p-2 p-4" -> "p-4").
 * Utilisé par tous les composants shadcn (components/ui/*) et par nos propres
 * composants dès qu'on accepte une prop `className` en override.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
