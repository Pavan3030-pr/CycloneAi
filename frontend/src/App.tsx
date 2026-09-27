import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/AppShell';
import { AdvisoryPage } from '@/pages/AdvisoryPage';
import { AssessmentPage } from '@/pages/AssessmentPage';
import { AssetsPage } from '@/pages/AssetsPage';
import { Dashboard } from '@/pages/Dashboard';
import { TrackVisualizer } from '@/pages/TrackVisualizer';

/**
 * Route table.
 *
 * Every console route sits inside {@link AppShell}, which owns the session gate, so a new screen is
 * protected by construction rather than by remembering to add a guard to it.
 */
export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Dashboard />} />
        <Route path="track" element={<TrackVisualizer />} />
        <Route path="assessment" element={<AssessmentPage />} />
        <Route path="assets" element={<AssetsPage />} />
        <Route path="advisory" element={<AdvisoryPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
