import { motion, useReducedMotion } from 'framer-motion';
import { AlertTriangle, Gauge, Navigation } from 'lucide-react';
import type { ReactNode } from 'react';

/**
 * The hero illustration.
 *
 * Not a stock photo and not a screenshot: it is the actual shape of the Bay of Bengal demonstration
 * scenario, drawn to scale from the same six fixes the console plots — 8.4°N 87.1°E in the southern
 * bay tracking north-west to landfall near Bapatla, with the coastal assets that the screening
 * model flags. Drawing real geometry rather than a decorative swirl is what makes the product look
 * like it means it: the coast bends where the Coromandel coast bends, and the risk colours on the
 * markers match the console's palette exactly.
 *
 * The storms rings and the floating readouts are animated with Framer Motion, which collapses to a
 * still image under `prefers-reduced-motion`.
 */

/** Map projection for the scene: equirectangular over 78.5°–89°E, 7°–17.5°N. */
const WIDTH = 800;
const HEIGHT = 560;
const project = (latitude: number, longitude: number): [number, number] => [
  ((longitude - 78.5) / 10.5) * WIDTH,
  ((17.5 - latitude) / 10.5) * HEIGHT,
];

const TRACK = [
  { latitude: 8.4, longitude: 87.1 },
  { latitude: 10.3, longitude: 85.4 },
  { latitude: 12.1, longitude: 83.6 },
  { latitude: 13.9, longitude: 81.8 },
  { latitude: 15.4, longitude: 80.5 },
  { latitude: 16.1, longitude: 80.1 },
];

const TRACK_PATH = TRACK.map((point, index) => {
  const [x, y] = project(point.latitude, point.longitude);
  return `${index === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
}).join(' ');

// The interpolated centre at the demonstration scenario's evaluation instant (2023-12-05T06:00Z),
// which is midway between the 00:00 and 12:00 fixes — 15.75°N 80.30°E, 48 kt, 994 mb.
const CENTRE = project(15.75, 80.3);

/** A representative slice of the assessed registry, coloured by the risk level the model returned. */
const ASSETS = [
  { id: 'bapatla', name: 'Bapatla shelter', at: project(15.9, 80.47), colour: '#e11d48', label: false },
  { id: 'nh16', name: 'NH-16 Krishna span', at: project(15.85, 80.62), colour: '#e11d48', label: false },
  { id: 'machilipatnam', name: 'Machilipatnam node', at: project(16.17, 81.13), colour: '#f97316', label: true },
  { id: 'nellore', name: 'Nellore grid node', at: project(14.44, 79.98), colour: '#f59e0b', label: false },
  { id: 'ennore', name: 'Ennore substation', at: project(13.23, 80.32), colour: '#10b981', label: false },
  { id: 'chennai', name: 'Chennai bypass', at: project(12.9, 80.15), colour: '#10b981', label: true },
];

export function HeroVisual() {
  const reduceMotion = useReducedMotion();
  const float = (offset: number) =>
    reduceMotion
      ? {}
      : {
          animate: { y: [0, -9, 0] },
          transition: { duration: 6 + offset, repeat: Infinity, ease: 'easeInOut' as const },
        };

  return (
    <div
      role="img"
      aria-label="Bay of Bengal storm track from the southern bay to landfall near Bapatla, with six assessed coastal assets coloured by risk level: two critical, one high, one medium and two low."
      className="relative overflow-hidden rounded-3xl border border-ink-200/80 bg-white shadow-lift"
    >
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="block w-full">
        <defs>
          <linearGradient id="hero-ocean" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#f6f9ff" />
            <stop offset="0.5" stopColor="#e8f0fe" />
            <stop offset="1" stopColor="#dfeafb" />
          </linearGradient>
          <linearGradient id="hero-land" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#ffffff" />
            <stop offset="1" stopColor="#eef1f7" />
          </linearGradient>
          <radialGradient id="hero-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0" stopColor="#2f5ae6" stopOpacity="0.34" />
            <stop offset="0.55" stopColor="#2fcbbb" stopOpacity="0.12" />
            <stop offset="1" stopColor="#2fcbbb" stopOpacity="0" />
          </radialGradient>
          <pattern id="hero-grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#111e49" strokeOpacity="0.05" strokeWidth="1" />
          </pattern>
          <pattern id="hero-wind" width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(38)">
            <path d="M 0 0 L 0 14" stroke="#2346c4" strokeOpacity="0.28" strokeWidth="1.4" />
          </pattern>
          <filter id="hero-shadow" x="-40%" y="-40%" width="180%" height="180%">
            <feDropShadow dx="0" dy="4" stdDeviation="6" floodColor="#111e49" floodOpacity="0.18" />
          </filter>
        </defs>

        <rect width={WIDTH} height={HEIGHT} fill="url(#hero-ocean)" />

        {/* Landmass: the Coromandel coast, drawn as one soft shape so it reads as geography, not chrome. */}
        <path
          d="M 0 0 H 156 C 138 78 118 142 110 204 C 102 268 120 330 133 382 C 146 434 152 492 122 560 H 0 Z"
          fill="url(#hero-land)"
        />
        <path
          d="M 156 0 C 138 78 118 142 110 204 C 102 268 120 330 133 382 C 146 434 152 492 122 560"
          fill="none"
          stroke="#c9d4e6"
          strokeWidth="1.6"
        />
        <rect width={WIDTH} height={HEIGHT} fill="url(#hero-grid)" />

        {/* Modelled wind field around the interpolated centre. */}
        <circle cx={CENTRE[0]} cy={CENTRE[1]} r="210" fill="url(#hero-glow)" />
        <circle cx={CENTRE[0]} cy={CENTRE[1]} r="96" fill="url(#hero-wind)" opacity="0.5" />
        <circle
          cx={CENTRE[0]}
          cy={CENTRE[1]}
          r="96"
          fill="none"
          stroke="#2346c4"
          strokeOpacity="0.35"
          strokeWidth="1.4"
          strokeDasharray="5 5"
        />

        {/* Forecast track, with fix markers growing along the intensification. */}
        <path d={TRACK_PATH} fill="none" stroke="#2f5ae6" strokeWidth="2.6" strokeDasharray="9 7" strokeLinecap="round" />
        {TRACK.map((point, index) => {
          const [x, y] = project(point.latitude, point.longitude);
          const radius = 4.5 + index * 1.1;
          return (
            <g key={`${point.latitude}-${point.longitude}`}>
              <circle cx={x} cy={y} r={radius} fill="#ffffff" stroke="#2346c4" strokeWidth="2.4" />
              <circle cx={x} cy={y} r="1.8" fill="#2346c4" />
            </g>
          );
        })}

        {/* The interpolated centre: pulsing rings, because it is a modelled position, not a fix. */}
        <motion.circle
          cx={CENTRE[0]}
          cy={CENTRE[1]}
          r="34"
          fill="none"
          stroke="#2f5ae6"
          strokeWidth="1.5"
          initial={{ opacity: 0.5, scale: 0.85 }}
          animate={reduceMotion ? { opacity: 0.4 } : { opacity: [0.55, 0, 0.55], scale: [0.85, 1.35, 0.85] }}
          transition={{ duration: 3.6, repeat: Infinity, ease: 'easeInOut' }}
          style={{ transformOrigin: `${CENTRE[0]}px ${CENTRE[1]}px` }}
        />
        <circle cx={CENTRE[0]} cy={CENTRE[1]} r="14" fill="#e0ebff" filter="url(#hero-shadow)" />
        <circle cx={CENTRE[0]} cy={CENTRE[1]} r="8.5" fill="#ffffff" stroke="#2346c4" strokeWidth="3" />
        <circle cx={CENTRE[0]} cy={CENTRE[1]} r="2.6" fill="#2fcbbb" />

        {/* Assessed assets, in the console's risk colours. */}
        {ASSETS.map((asset) => (
          <g key={asset.id}>
            <circle cx={asset.at[0]} cy={asset.at[1]} r="12" fill={asset.colour} opacity="0.14" />
            <circle cx={asset.at[0]} cy={asset.at[1]} r="5.6" fill="#ffffff" stroke={asset.colour} strokeWidth="3.2" />
            {asset.label ? (
              <text
                x={asset.at[0] + 14}
                y={asset.at[1] + 4}
                fill="#3b4354"
                fontSize="12.5"
                fontWeight="600"
                className="font-sans"
              >
                {asset.name}
              </text>
            ) : null}
          </g>
        ))}

        {/* Graticule labels, the one detail that makes a map look like a map. */}
        <text x="766" y="22" textAnchor="end" fill="#98a2b6" fontSize="11" fontWeight="600" className="font-mono">
          88°E
        </text>
        <text x="20" y="542" fill="#98a2b6" fontSize="11" fontWeight="600" className="font-mono">
          8°N
        </text>
      </svg>

      {/* Floating readouts, laid over the scene as HTML so they can use the real type scale. */}
      <motion.div
        {...float(0)}
        className="absolute left-4 top-4 flex items-center gap-2 rounded-full border border-white/70 bg-white/85 px-3.5 py-2 shadow-card backdrop-blur-sm"
      >
        <span className="relative flex size-2">
          <span className="absolute inline-flex size-full animate-ping rounded-full bg-accent-500 opacity-70" />
          <span className="relative inline-flex size-2 rounded-full bg-accent-500" />
        </span>        <span className="text-xs font-semibold text-ink-700">Screening live · IO-DEMO-01</span>
      </motion.div>

      <motion.div
        {...float(1.4)}
        className="absolute right-4 top-4 hidden w-[13.5rem] rounded-2xl border border-white/70 bg-white/88 p-3.5 shadow-card backdrop-blur-sm sm:block"
      >
        <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
          <Navigation className="size-3.5 text-brand-600" />
          Interpolated centre
        </div>
        <p className="mt-1.5 font-mono text-sm font-semibold text-ink-900">15.75°N 80.30°E</p>
        <div className="mt-2 flex items-center justify-between text-xs">
          <span className="flex items-center gap-1.5 text-ink-600">
            <Gauge className="size-3.5 text-brand-600" />
            48 kt
          </span>
          <span className="rounded-md bg-brand-50 px-1.5 py-0.5 font-mono text-[11px] font-semibold text-brand-700">
            TS · 994 mb
          </span>
        </div>
      </motion.div>

      <motion.div
        {...float(2.1)}
        className="absolute bottom-4 left-4 w-[15.5rem] rounded-2xl border border-white/70 bg-white/90 p-4 shadow-card backdrop-blur-sm"
      >
        <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
          <AlertTriangle className="size-3.5 text-coral-500" />
          Assets requiring action
        </div>
        <p className="mt-1.5 font-display text-2xl font-bold tracking-tight text-ink-900">
          2<span className="ml-1.5 text-sm font-semibold text-ink-500">of 7 assessed</span>
        </p>
        <div className="mt-2.5 space-y-1.5">
          <RiskRow colour="#e11d48" label="Critical" count={2} />
          <RiskRow colour="#f97316" label="High" count={1} />
          <RiskRow colour="#f59e0b" label="Medium" count={1} />
          <RiskRow colour="#10b981" label="Low" count={3} />
        </div>
      </motion.div>
    </div>
  );
}

function RiskRow({ colour, label, count }: { colour: string; label: string; count: number }): ReactNode {
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="size-2 rounded-full" style={{ background: colour }} />
      <span className="flex-1 text-ink-600">{label}</span>
      <span className="font-mono font-semibold text-ink-800">{count}</span>
    </div>
  );
}
