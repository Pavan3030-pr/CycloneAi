import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Logo } from '@/components/Logo';

/**
 * Route table, with every screen split into its own chunk.
 *
 * The split is not cosmetic: Leaflet, React Query's console screens and the assessment forms are all
 * dead weight to a visitor reading the landing page on a phone in a coastal district, and the
 * landing page is the only screen a first-time visitor loads. Deferring everything behind `lazy()`
 * keeps that first load to the marketing bundle, and the console arrives while the sign-in request
 * is in flight.
 */
const LandingPage = lazy(async () => ({ default: (await import('@/pages/LandingPage')).LandingPage }));
const SignInPage = lazy(async () => ({ default: (await import('@/pages/SignInPage')).SignInPage }));
const SignUpPage = lazy(async () => ({ default: (await import('@/pages/SignUpPage')).SignUpPage }));
const AppShell = lazy(async () => ({ default: (await import('@/components/AppShell')).AppShell }));
const Dashboard = lazy(async () => ({ default: (await import('@/pages/Dashboard')).Dashboard }));
const TrackVisualizer = lazy(async () => ({ default: (await import('@/pages/TrackVisualizer')).TrackVisualizer }));
const AssessmentPage = lazy(async () => ({ default: (await import('@/pages/AssessmentPage')).AssessmentPage }));
const AssetsPage = lazy(async () => ({ default: (await import('@/pages/AssetsPage')).AssetsPage }));
const AdvisoryPage = lazy(async () => ({ default: (await import('@/pages/AdvisoryPage')).AdvisoryPage }));

/**
 * The two halves of the product: the public site at the root, and the console under `/app`, where
 * every screen sits inside {@link AppShell} and is protected by construction rather than by
 * remembering to add a guard to it. Unknown paths fall back to the landing page rather than a bare
 * 404, because a mistyped URL from an email is more likely to be a stale link than a mistake.
 */
export default function App() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/signin" element={<SignInPage />} />
        <Route path="/signup" element={<SignUpPage />} />

        <Route path="/app" element={<AppShell />}>
          <Route index element={<Dashboard />} />
          <Route path="track" element={<TrackVisualizer />} />
          <Route path="assessment" element={<AssessmentPage />} />
          <Route path="assets" element={<AssetsPage />} />
          <Route path="advisory" element={<AdvisoryPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

/**
 * Shown while a route chunk downloads.
 *
 * Deliberately static — no spinner animation frame budget for a gap that is usually one round trip —
 * and sized to the viewport so the footer does not jump into view and back.
 */
function RouteFallback() {
  return (
    <div className="grid min-h-screen place-items-center bg-ink-50">
      <div className="flex flex-col items-center gap-4 text-center">
        <Logo className="h-9" />
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-ink-500">Loading</p>
      </div>
    </div>
  );
}
