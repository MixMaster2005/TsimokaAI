import { createFileRoute, Outlet } from '@tanstack/react-router';

export const Route = createFileRoute('/onboarding')({
  component: OnboardingLayout,
});

function OnboardingLayout() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-papier-bg px-6">
      <Outlet />
    </div>
  );
}
