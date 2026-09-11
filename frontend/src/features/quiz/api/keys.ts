export const quizKeys = {
  all: ['quizzes'] as const,
  bySpace: (spaceId: string) => [...quizKeys.all, 'space', spaceId] as const,
  mine: () => [...quizKeys.all, 'mine'] as const,
  detail: (id: string) => [...quizKeys.all, 'detail', id] as const,
  attempts: (id: string) => [...quizKeys.all, 'attempts', id] as const,
  stats: (id: string) => [...quizKeys.all, 'stats', id] as const,
  espace: (spaceId: string) => [...quizKeys.all, 'espace', spaceId] as const,
  corrections: (id: string) => [...quizKeys.all, 'corrections', id] as const,
  attemptCorrections: (attemptId: string) => [...quizKeys.all, 'attempt-corrections', attemptId] as const,
  shares: (id: string) => [...quizKeys.all, 'shares', id] as const,
};
