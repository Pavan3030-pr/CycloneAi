import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/AppShell';
import { AdvisoryPage } from '@/pages/AdvisoryPage';
import { AssessmentPage } from '@/pages/AssessmentPage';
import { AssetsPage } from '@/pages/AssetsPage';
import { Dashboard } from '@/pages/Dashboard';
import { LandingPage } from '@/pages/LandingPage';
import { SignInPage } from '@/pages/SignInPage';
import { SignUpPage } from '@/pages/SignUpPage';
import { TrackVisualizer } from '@/pages/TrackVisualizer';

/**
 * Route table.
 *
 * Two halves: the public site at the root, and the console under `/app`, where every screen sits
 * inside {@link AppShell} and is protected by construction rather than by remembering to add a guard
 * to it. Unknown paths fall back to the landing page rather than a bare 404, because a mistyped URL
 * from an email is more likely to be a stale link than a mistake.
 */
export default function App() {
  return (
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
  );
}
