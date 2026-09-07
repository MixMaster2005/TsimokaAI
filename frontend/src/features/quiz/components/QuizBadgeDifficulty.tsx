import { CheckCircle, AlertTriangle, XCircle } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import type { QuizDifficulty } from '../types';

interface QuizBadgeDifficultyProps {
  difficulty: QuizDifficulty;
}

const config: Record<QuizDifficulty, { variant: 'succes' | 'attention' | 'erreur'; icon: React.ReactNode; label: string }> = {
  FACILE: { variant: 'succes', icon: <CheckCircle className="mr-1 size-3" />, label: 'Facile' },
  MOYEN: { variant: 'attention', icon: <AlertTriangle className="mr-1 size-3" />, label: 'Moyen' },
  DIFFICILE: { variant: 'erreur', icon: <XCircle className="mr-1 size-3" />, label: 'Difficile' },
};

export function QuizBadgeDifficulty({ difficulty }: QuizBadgeDifficultyProps) {
  const { variant, icon, label } = config[difficulty];
  return (
    <Badge variant={variant}>
      {icon}
      {label}
    </Badge>
  );
}
