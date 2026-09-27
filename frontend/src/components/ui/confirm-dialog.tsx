import type { ReactNode } from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  onConfirm: () => void;
  isPending?: boolean;
}

/**
 * Confirmation standard des actions destructives (Lot 1 P0).
 * Pattern aligné sur `fiches-a-valider` : nom + conséquences + Annuler en focus par défaut.
 * Ne jamais utiliser `window.confirm` (bloque AT, sans style/focus).
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  cancelLabel = 'Annuler',
  onConfirm,
  isPending = false,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)} autoFocus>
            {cancelLabel}
          </Button>
          <Button variant="destructive" size="sm" disabled={isPending} onClick={onConfirm}>
            {isPending ? 'Suppression…' : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
