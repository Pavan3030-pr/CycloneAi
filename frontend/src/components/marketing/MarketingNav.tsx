import { AnimatePresence, motion } from 'framer-motion';
import { ArrowRight, Menu, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Logo } from '@/components/Logo';
import { useAuth } from '@/auth/AuthProvider';
import { cn } from '@/lib/cn';

/**
 * The public navbar.
 *
 * It starts transparent so the hero can bleed into the top of the page, and turns into a solid,
 * bordered bar the moment the user scrolls — the visual cue that the page has a fixed frame. The
 * state is driven by a single scroll listener rather than by an intersection observer, because the
 * threshold is a scroll distance, not an element.
 */

const LINKS = [
  { href: '#platform', label: 'Platform' },
  { href: '#how-it-works', label: 'How it works' },
  { href: '#trust', label: 'Trust & data' },
];

export function MarketingNav() {
  const { isAuthenticated } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className={cn(
        'fixed inset-x-0 top-0 z-50 transition-all duration-300 ease-premium',
        scrolled ? 'border-b border-ink-200/70 bg-white/85 backdrop-blur-xl' : 'border-b border-transparent',
      )}
    >
      <div className="container-page flex h-16 items-center justify-between gap-6 sm:h-18">
        <Link to="/" aria-label="CycloneAI home" className="rounded-lg">
          <Logo />
        </Link>

        <nav className="hidden items-center gap-1 lg:flex" aria-label="Sections">
          {LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="rounded-lg px-3 py-2 text-sm font-medium text-ink-600 transition hover:bg-ink-100 hover:text-ink-900"
            >
              {link.label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-2.5 lg:flex">
          {isAuthenticated ? (
            <Link
              to="/app"
              className="btn bg-brand-700 text-white shadow-glow hover:bg-brand-800"
            >
              Open console
              <ArrowRight className="size-4" />
            </Link>
          ) : (
            <>
              <Link to="/signin" className="btn text-ink-700 hover:bg-ink-100">
                Sign in
              </Link>
              <Link to="/signup" className="btn bg-brand-700 text-white shadow-glow hover:bg-brand-800">
                Get started
                <ArrowRight className="size-4" />
              </Link>
            </>
          )}
        </div>

        <button
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          className="btn border border-ink-200 bg-white px-3 text-ink-700 lg:hidden"
        >
          {menuOpen ? <X className="size-4" /> : <Menu className="size-4" />}
        </button>
      </div>

      <AnimatePresence>
        {menuOpen ? (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
            className="overflow-hidden border-t border-ink-200/70 bg-white/95 backdrop-blur-xl lg:hidden"
          >
            <div className="container-page flex flex-col gap-1 py-4">
              {LINKS.map((link) => (
                <a
                  key={link.href}
                  href={link.href}
                  onClick={() => setMenuOpen(false)}
                  className="rounded-lg px-3 py-2.5 text-sm font-medium text-ink-700 transition hover:bg-ink-100"
                >
                  {link.label}
                </a>
              ))}
              <div className="mt-2 flex flex-col gap-2">
                {isAuthenticated ? (
                  <Link to="/app" className="btn bg-brand-700 text-white">
                    Open console
                  </Link>
                ) : (
                  <>
                    <Link to="/signin" className="btn border border-ink-200 bg-white text-ink-800">
                      Sign in
                    </Link>
                    <Link to="/signup" className="btn bg-brand-700 text-white">
                      Get started free
                    </Link>
                  </>
                )}
              </div>
            </div>
          </motion.div>
        ) : null}
      </AnimatePresence>
    </header>
  );
}
