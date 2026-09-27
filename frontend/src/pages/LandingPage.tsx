import { motion } from 'framer-motion';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  Compass,
  Database,
  FileText,
  Gauge,
  Layers,
  Lock,
  PlayCircle,
  Radar,
  Route,
  Send,
  ShieldCheck,
  Sparkles,
  Timer,
  Waypoints,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { HeroVisual } from '@/components/marketing/HeroVisual';
import { MarketingFooter } from '@/components/marketing/MarketingFooter';
import { MarketingNav } from '@/components/marketing/MarketingNav';
import { Reveal, staggerContainer, staggerItem } from '@/components/Reveal';
import { cn } from '@/lib/cn';

/**
 * The public landing page.
 *
 * Ordered as a single argument rather than a feature list: what the product does (hero), who relies
 * on it (proof), what it actually computes (platform), how a team would adopt it (steps), why a
 * government buyer can trust it (trust band), and the one thing to do next (closing CTA). The
 * demonstration link carries `?demo=1`, which the sign-in screen uses to run the real Bay of Bengal
 * scenario automatically, so "Start free demo" leads to real server output and not a video.
 */
export function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      <MarketingNav />
      <main>
        <Hero />
        <SocialProof />
        <Platform />
        <HowItWorks />
        <TrustBand />
        <Roadmap />
        <ClosingCta />
      </main>
      <MarketingFooter />
    </div>
  );
}

function Hero() {
  return (
    <section className="relative overflow-hidden bg-aurora pb-16 pt-28 sm:pb-24 sm:pt-32 lg:pb-28 lg:pt-40">
      {/* A faint engineering grid, fading out under the fold so it never competes with the copy. */}
      <div className="pointer-events-none absolute inset-0 bg-grid-faint bg-grid fade-bottom opacity-70" />
      <div className="container-page relative grid items-center gap-14 lg:grid-cols-[1.02fr_1.15fr] lg:gap-16">
        <div>
          <Reveal>
            <span className="eyebrow rounded-full border border-brand-200/80 bg-white/80 px-3 py-1.5 shadow-hair backdrop-blur-sm">
              <Radar className="size-3.5" />
              Anticipatory intelligence for critical infrastructure
            </span>
          </Reveal>

          <Reveal delay={0.06}>
            <h1 className="mt-6 font-display text-[2.6rem] font-extrabold leading-[1.05] tracking-tightest text-ink-900 sm:text-6xl">
              See the storm
              <br className="hidden sm:block" /> before it hits.
            </h1>
          </Reveal>

          <Reveal delay={0.12}>
            <p className="mt-6 max-w-xl text-lg leading-relaxed text-ink-600">
              CycloneAI turns published cyclone tracks into asset-level impact forecasts for power grids,
              arterial roads and medical shelters. Feed it a fix or a GeoJSON document and get back which
              assets need action, how hard the wind will hit them, and the advisory text to send.
            </p>
          </Reveal>

          <Reveal delay={0.18}>
            <div className="mt-9 flex flex-wrap items-center gap-3">
              <Link
                to="/signin?demo=1"
                className="btn bg-brand-700 px-5 py-3 text-base text-white shadow-glow transition hover:bg-brand-800"
              >
                Start free demo
                <ArrowRight className="size-4.5" />
              </Link>
              <Link
                to="/signin"
                className="btn border border-ink-200 bg-white/90 px-5 py-3 text-base text-ink-800 shadow-hair backdrop-blur-sm transition hover:border-ink-300 hover:bg-white"
              >
                <PlayCircle className="size-5 text-brand-600" />
                Sign in to the console
              </Link>
            </div>
          </Reveal>

          <Reveal delay={0.24}>
            <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2.5 text-sm text-ink-600">
              {['Runs the real screening model', 'No card, no deploy required', 'ISO 8601 UTC end to end'].map(
                (item) => (
                  <li key={item} className="flex items-center gap-2">
                    <CheckCircle2 className="size-4 text-accent-600" />
                    {item}
                  </li>
                ),
              )}
            </ul>
          </Reveal>
        </div>

        <Reveal delay={0.1}>
          <HeroVisual />
        </Reveal>
      </div>
    </section>
  );
}

const PROGRAMMES = [
  'Coastal Resilience Cell',
  'Met-Ops Forecasting Desk',
  'State Disaster Authority',
  'Transmission Grid Authority',
  'Port & Harbour Trust',
  'Regional Health Directorate',
];

function SocialProof() {
  return (
    <section className="border-y border-ink-200/70 bg-ink-50/70">
      <div className="container-page py-10">
        <p className="text-center text-xs font-semibold uppercase tracking-[0.16em] text-ink-500">
          Built for the desks that own the coastline
        </p>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-x-10 gap-y-4">
          {PROGRAMMES.map((programme) => (
            <span
              key={programme}
              className="font-display text-base font-bold tracking-tight text-ink-400 transition hover:text-ink-600"
            >
              {programme}
            </span>
          ))}
        </div>
        <p className="mt-6 text-center text-[11px] text-ink-400">
          Programme marks are placeholders demonstrating layout; no endorsement is implied.
        </p>
      </div>
    </section>
  );
}

const FEATURES = [
  {
    icon: Route,
    title: 'Track simulation',
    tone: 'brand' as const,
    body: 'Publish a fix series or paste an agency GeoJSON document. The track is canonicalised, gaps are interpolated and the storm centre is placed at any instant you ask for — including between fixes.',
    points: ['Six-hourly or irregular fixes', 'ATC FIX / RFC 3339 timestamps', 'Antimeridian-safe interpolation'],
  },
  {
    icon: Building2,
    title: 'Infrastructure exposure',
    tone: 'accent' as const,
    body: 'Every registered asset is screened against the modelled wind field with a 75 nm exponential decay, then banded into critical, high, medium or low — with the rule that fired stated per asset.',
    points: ['Grid, arterial road, shelter types', 'Distance and modelled wind per asset', 'Explained, not just scored'],
  },
  {
    icon: Sparkles,
    title: 'Gemini-written advisories',
    tone: 'coral' as const,
    body: 'Gemini drafts the briefing from the assessment facts — storm identity, validity time, intensity and the assets needing action — in English, Hindi or Telugu. Every advisory states who wrote it and how long it took.',
    points: ['Provenance shown on every response', 'Deterministic fallback if the model fails', 'A template never silently replaces the model'],
  },
  {
    icon: Send,
    title: 'Into the systems that warn people',
    tone: 'accent' as const,
    body: 'Every assessment renders as a CAP 1.2 alert other warning platforms can ingest, and can be published to a messaging gateway or control-room webhook when one is configured for the deployment.',
    points: ['Standards-compliant CAP 1.2 download', 'Dispatch gated by configuration and role', 'Fails loudly, never half-sends'],
  },
];

const TONES = {
  brand: { chip: 'bg-brand-50 text-brand-700 ring-brand-100', icon: 'text-brand-700' },
  accent: { chip: 'bg-accent-50 text-accent-700 ring-accent-100', icon: 'text-accent-700' },
  coral: { chip: 'bg-coral-50 text-coral-700 ring-coral-100', icon: 'text-coral-700' },
};

function Platform() {
  return (
    <section id="platform" className="section">
      <div className="container-page">
        <SectionHeading
          eyebrow="The platform"
          title="Four capabilities, one screening pipeline"
          body="Each piece is independently useful and they share the same track, asset registry and exposure model — so a number on the map is the number in the advisory."
        />

        <motion.div
          variants={staggerContainer}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-90px' }}
          className="mt-14 grid gap-5 sm:grid-cols-2 lg:gap-6"
        >
          {FEATURES.map((feature) => (
            <motion.article
              key={feature.title}
              variants={staggerItem}
              className="group card relative overflow-hidden p-6 transition duration-300 ease-premium hover:-translate-y-1 hover:border-brand-200 hover:shadow-lift sm:p-7"
            >
              <div
                className={cn(
                  'inline-flex size-11 items-center justify-center rounded-xl ring-1 ring-inset',
                  TONES[feature.tone].chip,
                )}
              >
                <feature.icon className="size-5" />
              </div>
              <h3 className="mt-5 font-display text-lg font-bold tracking-tight text-ink-900">{feature.title}</h3>
              <p className="mt-2.5 text-sm leading-relaxed text-ink-600">{feature.body}</p>
              <ul className="mt-5 space-y-2 border-t border-ink-100 pt-5">
                {feature.points.map((point) => (
                  <li key={point} className="flex items-start gap-2 text-xs font-medium text-ink-600">
                    <CheckCircle2 className={cn('mt-0.5 size-3.5 shrink-0', TONES[feature.tone].icon)} />
                    {point}
                  </li>
                ))}
              </ul>
            </motion.article>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

const STEPS = [
  {
    icon: Waypoints,
    title: 'Bring the track',
    body: 'Submit the published fixes — structured JSON or an agency FeatureCollection. Nothing is reshaped by hand: the server understands NHC, JTWC and IMD property names.',
  },
  {
    icon: Gauge,
    title: 'Screen the assets',
    body: 'Point it at your registry of grids, roads and shelters. Choose the instant to evaluate, including mid-interval points where the track is interpolated rather than observed.',
  },
  {
    icon: FileText,
    title: 'Issue the advisory',
    body: 'Read the exposure table with the rule behind every level, then download the CAP 1.2 alert or publish the advisory to the configured channel — and keep the provenance line that says whether Gemini or the template wrote it.',
  },
];

function HowItWorks() {
  return (
    <section id="how-it-works" className="border-y border-ink-200/70 bg-ink-50/60 section">
      <div className="container-page">
        <SectionHeading
          eyebrow="How it works"
          title="From a published fix to a briefing in three moves"
          body="No modelling degree required, and no black box: every level the model returns can be traced back to a stated threshold."
        />

        <div className="mt-14 grid gap-6 lg:grid-cols-3">
          {STEPS.map((step, index) => (
            <Reveal key={step.title} delay={index * 0.08} className="relative">
              <div className="card h-full p-7">
                <div className="flex items-center justify-between">
                  <span className="inline-flex size-11 items-center justify-center rounded-xl bg-brand-700 text-white shadow-glow">
                    <step.icon className="size-5" />
                  </span>
                  <span className="font-display text-4xl font-extrabold tracking-tightest text-ink-100">
                    0{index + 1}
                  </span>
                </div>
                <h3 className="mt-5 font-display text-lg font-bold tracking-tight text-ink-900">{step.title}</h3>
                <p className="mt-2.5 text-sm leading-relaxed text-ink-600">{step.body}</p>
              </div>
              {index < STEPS.length - 1 ? (
                <ArrowRight className="absolute -right-5 top-1/2 hidden size-5 -translate-y-1/2 text-ink-300 lg:block" />
              ) : null}
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

const SECURITY = [
  { icon: Lock, title: 'Role-based access', body: 'ADMIN, ANALYST and VIEWER are enforced at the API, not in the interface.' },
  { icon: ShieldCheck, title: 'Hardened by default', body: 'Strict CSP, frame denial, no sniffing, and short-lived bearer tokens.' },
  { icon: Timer, title: 'Rate limited and traceable', body: 'Per-principal throttling with Retry-After, and a correlation id on every request.' },
  { icon: Database, title: 'Deterministic, UTC-pinned', body: 'Identical input returns identical output, and every instant is UTC all the way down.' },
];

const THRESHOLDS = [
  { level: 'Critical', rule: '≥ 64 kt wind or ≤ 30 nm from centre', colour: '#e11d48' },
  { level: 'High', rule: '≥ 50 kt wind or ≤ 60 nm from centre', colour: '#f97316' },
  { level: 'Medium', rule: '≥ 34 kt wind or ≤ 120 nm from centre', colour: '#f59e0b' },
  { level: 'Low', rule: 'Below every band — monitored, no action', colour: '#10b981' },
];

/**
 * What this build deliberately does not do yet.
 *
 * Stated on the marketing page because the alternative — implying a satellite pipeline that does not
 * exist — is the one claim in climate software that damages a team's credibility fastest. A product
 * that names its own boundary reads as engineering, not marketing.
 */
const ROADMAP = [
  {
    title: 'Earth observation cross-checks',
    body: 'A Google Earth Engine adapter that verifies reported intensity against imagery and, later, measures coastal inundation extent.',
  },
  {
    title: 'Storm surge and rainfall',
    body: 'A second hazard field alongside modelled wind, so an asset can be flagged for surge depth as well as wind speed.',
  },
  {
    title: 'Persistent history',
    body: 'Assessments, advisories and dispatch receipts stored against a real database instead of living in the service that produced them.',
  },
];

function Roadmap() {
  return (
    <section id="roadmap" className="border-t border-ink-200/70 bg-ink-50/60 section">
      <div className="container-page">
        <SectionHeading
          eyebrow="Where this build stops"
          title="What the platform does not claim yet"
          body="The screening model, the AI advisory path, CAP 1.2 output and the demo you just ran are all live in this build. These three are not, and the interface does not pretend otherwise."
        />

        <div className="mt-12 grid gap-5 lg:grid-cols-3">
          {ROADMAP.map((item, index) => (
            <Reveal key={item.title} delay={index * 0.07}>
              <div className="h-full rounded-2xl border border-dashed border-ink-300 bg-white/70 p-6">
                <span className="inline-flex items-center gap-2 rounded-full bg-ink-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
                  <Compass className="size-3.5" />
                  Planned
                </span>
                <h3 className="mt-4 font-display text-base font-bold tracking-tight text-ink-900">{item.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-600">{item.body}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

function TrustBand() {
  return (
    <section id="trust" className="section">
      <div className="container-page grid gap-12 lg:grid-cols-2 lg:gap-16">
        <div>
          <SectionHeading
            eyebrow="Trust & data"
            title="Built to survive a procurement review"
            body="Climate software is judged on whether an operations team can defend the numbers afterwards. Access control, tracing and reproducibility are core, not features."
            align="left"
          />
          <div className="mt-9 grid gap-5 sm:grid-cols-2">
            {SECURITY.map((item, index) => (
              <Reveal key={item.title} delay={index * 0.06}>
                <div className="flex gap-3">
                  <span className="mt-0.5 inline-flex size-9 shrink-0 items-center justify-center rounded-lg bg-white text-brand-700 shadow-hair ring-1 ring-inset ring-ink-200">
                    <item.icon className="size-4.5" />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-ink-900">{item.title}</p>
                    <p className="mt-1 text-xs leading-relaxed text-ink-600">{item.body}</p>
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </div>

        <Reveal delay={0.08}>
          <div className="card overflow-hidden">
            <div className="border-b border-ink-100 bg-ink-50/70 px-6 py-4">
              <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-ink-500">
                Screening rules, in full
              </p>
            </div>
            <ul className="divide-y divide-ink-100">
              {THRESHOLDS.map((threshold) => (
                <li key={threshold.level} className="flex items-start gap-3 px-6 py-4">
                  <span
                    className="mt-1.5 size-2.5 shrink-0 rounded-full"
                    style={{ background: threshold.colour }}
                  />
                  <div>
                    <p className="text-sm font-semibold text-ink-900">{threshold.level}</p>
                    <p className="mt-0.5 font-mono text-xs text-ink-600">{threshold.rule}</p>
                  </div>
                </li>
              ))}
            </ul>
            <div className="border-t border-ink-100 bg-ink-50/70 px-6 py-4 text-xs leading-relaxed text-ink-500">
              Modelled wind decays exponentially from the storm's maximum sustained wind with a 75 nm
              e-folding scale. Terrain, gust factor, quadrant asymmetry and forecast positional
              uncertainty are deliberately out of scope — the model stays legible instead of pretending
              to be complete.
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function ClosingCta() {
  return (
    <section className="pb-20 sm:pb-28">
      <div className="container-page">
        <div className="relative overflow-hidden rounded-3xl bg-brand-sheen px-6 py-14 shadow-lift sm:px-14 sm:py-16">
          <div className="pointer-events-none absolute -right-24 -top-24 size-[26rem] rounded-full bg-accent-400/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-32 -left-20 size-[24rem] rounded-full bg-brand-400/25 blur-3xl" />

          <div className="relative flex flex-col items-start gap-10 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-2xl">
              <span className="inline-flex items-center gap-2 rounded-full bg-white/12 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-accent-200">
                <Layers className="size-3.5" />
                Live demonstration
              </span>
              <h2 className="mt-5 font-display text-3xl font-extrabold leading-tight tracking-tightest text-white sm:text-4xl">
                Watch a real storm get assessed in under a minute.
              </h2>
              <p className="mt-4 text-base leading-relaxed text-brand-100">
                One click loads a six-fix Bay of Bengal track and seven real coastal assets through the
                live API, then hands you the risk summary and the advisory it generated.
              </p>
            </div>

            <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
              <Link
                to="/signin?demo=1"
                className="btn bg-white px-6 py-3.5 text-base text-brand-800 shadow-lift transition hover:bg-brand-50"
              >
                Run the demo storm
                <ArrowRight className="size-4.5" />
              </Link>
              <Link
                to="/signup"
                className="btn border border-white/25 bg-white/10 px-6 py-3.5 text-base text-white transition hover:bg-white/20"
              >
                Create an account
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function SectionHeading({
  eyebrow,
  title,
  body,
  align = 'center',
}: {
  eyebrow: string;
  title: string;
  body: string;
  align?: 'center' | 'left';
}): ReactNode {
  return (
    <Reveal className={cn('max-w-2xl', align === 'center' && 'mx-auto text-center')}>
      <span className="eyebrow">{eyebrow}</span>
      <h2 className="mt-4 font-display text-3xl font-extrabold tracking-tightest text-ink-900 sm:text-4xl">
        {title}
      </h2>
      <p className="mt-4 text-base leading-relaxed text-ink-600">{body}</p>
    </Reveal>
  );
}
