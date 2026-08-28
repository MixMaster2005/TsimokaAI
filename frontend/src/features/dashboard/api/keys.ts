export const dashboardKeys = {
  student: (spaceId: string) => ['dashboard', 'student', spaceId] as const,
  teacher: (spaceId: string) => ['dashboard', 'teacher', spaceId] as const,
};

export const recommandationsKeys = {
  bySpace: (spaceId: string) => ['recommandations', spaceId] as const,
};
