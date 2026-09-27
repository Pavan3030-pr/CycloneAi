import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthProvider';
import 'leaflet/dist/leaflet.css';
import './index.css';

/**
 * Composition root: router, server-state cache and session, in that order.
 *
 * Queries are retried once and not refetched on window focus. For a dashboard watching one storm
 * that is the right trade: a refetch storm on every alt-tab adds load without adding certainty, and
 * the demo relies on the data staying still while someone is talking over it.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 15_000,
    },
    mutations: {
      retry: 0,
    },
  },
});

const rootElement = document.getElementById('root');
if (rootElement === null) {
  throw new Error('Root element is missing from index.html');
}

createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      {/* Opting into the v7 behaviours now keeps the console free of deprecation warnings and\n          removes a migration step from the next upgrade. */}
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
