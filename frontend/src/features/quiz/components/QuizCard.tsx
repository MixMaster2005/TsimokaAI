import { Link } from '@tanstack/react-router';
import { FileQuestion } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { Quiz } from '../types';
import { QuizBadgeDifficulty } from './QuizBadgeDifficulty';

interface QuizCardProps {
  quiz: Quiz;
  spaceId: string;
  className?: string;
  basePath?: string;
}

export function QuizCard({ quiz, spaceId, className, basePath = '/espaces/$spaceId' }: QuizCardProps) {
  return (
    <Link
      to={basePath === '/enseignant' ? '/enseignant/espaces/$spaceId/quiz/$quizId' : '/espaces/$spaceId/quiz/$quizId'}
      params={{ spaceId, quizId: quiz.id }}
      className={cn(
        'flex overflow-hidden rounded-fiche border border-papier-border bg-papier-carte shadow-sm transition-colors hover:bg-papier-bg',
        className,
      )}
    >
      <div className="w-2 flex-none bg-tag-info" />
      <div className="flex-1 p-5">
        <p className="font-mono text-[0.65rem] uppercase tracking-wide text-encre-muted">Quiz</p>
        <h3 className="mb-2 font-display text-xl font-semibold text-encre">{quiz.title ?? 'Quiz sans titre'}</h3>

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <QuizBadgeDifficulty difficulty={quiz.difficulty} />
          {quiz.statut === 'BROUILLON' && <Badge variant="outline">Brouillon</Badge>}
          <Badge variant="secondary">
            <FileQuestion className="mr-1 size-3" />
            {quiz.questionCount} question{quiz.questionCount > 1 ? 's' : ''}
          </Badge>
        </div>

        <div className="border-t border-dashed border-papier-border pt-3 font-mono text-[0.68rem] text-encre-muted">
          {quiz.obsolete && <span className="text-attention">Obsolète · </span>}
          {quiz.sourceDocumentIds.length} document{quiz.sourceDocumentIds.length > 1 ? 's' : ''}
        </div>
      </div>
    </Link>
  );
}
