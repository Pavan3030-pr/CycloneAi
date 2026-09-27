import { Link } from 'react-router-dom';
import { Logo } from '@/components/Logo';

/**
 * Site footer.
 *
 * The disclaimer lives here rather than being buried in a modal: this product produces text that
 * someone may act on, so the fact that it is a screening model and not an official warning has to be
 * visible on every public page.
 */

const COLUMNS = [
  {
    title: 'Platform',
    links: [
      { label: 'Track simulation', href: '#platform' },
      { label: 'Infrastructure exposure', href: '#platform' },
      { label: 'Early-warning advisories', href: '#platform' },
      { label: 'How it works', href: '#how-it-works' },
    ],
  },
  {
    title: 'Console',
    links: [
      { label: 'Sign in', href: '/signin' },
      { label: 'Create an account', href: '/signup' },
      { label: 'Documentation', href: '#trust' },
      { label: 'API reference', href: '#trust' },
    ],
  },
  {
    title: 'Programme',
    links: [
      { label: 'Trust & data sources', href: '#trust' },
      { label: 'Security posture', href: '#trust' },
      { label: 'Accessibility', href: '#trust' },
      { label: 'Contact the team', href: '#trust' },
    ],
  },
];

export function MarketingFooter() {
  return (
    <footer className="border-t border-ink-200/80 bg-white">
      <div className="container-page grid gap-12 py-14 lg:grid-cols-[1.4fr_repeat(3,1fr)]">
        <div className="max-w-sm">
          <Logo />
          <p className="mt-4 text-sm leading-relaxed text-ink-600">
            CycloneAI turns published cyclone tracks into asset-level impact forecasts for power grids,
            arterial roads and medical shelters — minutes after a fix is issued.
          </p>
          <p className="mt-4 text-xs leading-relaxed text-ink-500">
            Screening model. Cross-check against official NHC, JTWC and IMD products before operational use.
          </p>
        </div>

        {COLUMNS.map((column) => (
          <div key={column.title}>
            <h3 className="text-[11px] font-semibold uppercase tracking-[0.14em] text-ink-500">{column.title}</h3>
            <ul className="mt-4 space-y-2.5">
              {column.links.map((link) => (
                <li key={link.label}>
                  {link.href.startsWith('/') ? (
                    <Link to={link.href} className="text-sm text-ink-600 transition hover:text-brand-700">
                      {link.label}
                    </Link>
                  ) : (
                    <a href={link.href} className="text-sm text-ink-600 transition hover:text-brand-700">
                      {link.label}
                    </a>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className="border-t border-ink-200/80">
        <div className="container-page flex flex-col items-start justify-between gap-2 py-5 text-xs text-ink-500 sm:flex-row sm:items-center">
          <p>© {new Date().getFullYear()} CycloneAI. Built for coastal resilience programmes.</p>
          <p className="font-mono">v1.0 · Anticipatory intelligence for critical infrastructure</p>
        </div>
      </div>
    </footer>
  );
}
