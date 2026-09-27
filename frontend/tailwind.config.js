/** @type {import('tailwindcss').Config} */

/**
 * The design system.
 *
 * Light theme only: there is no `darkMode` key and no `dark:` variant anywhere in the app, which is
 * deliberate. A screening console is read on a bright wall in a control room and on a phone in the
 * field, and one well-tuned light theme beats two mediocre ones.
 *
 * Three colour families do all the work:
 *   - `brand`  deep, institutional blue: navigation, primary actions, headings.
 *   - `accent` teal: the "anticipatory" half of the product, used for positive/ready states.
 *   - `coral`  the alert ramp, kept separate from the risk palette so a decorative red can never be
 *              mistaken for a critical risk level.
 */

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f1f6ff',
          100: '#e0ebff',
          200: '#c7daff',
          300: '#a2c1ff',
          400: '#779ffb',
          500: '#4f7bf5',
          600: '#2f5ae6',
          700: '#2346c4',
          800: '#1f3b9c',
          900: '#1c3479',
          950: '#111e49',
        },
        accent: {
          50: '#eefdfa',
          100: '#cbfaf1',
          200: '#9af2e4',
          300: '#61e3d3',
          400: '#2fcbbb',
          500: '#14ada0',
          600: '#0a8b82',
          700: '#0c6f69',
          800: '#0e5956',
          900: '#104a48',
          950: '#032b2b',
        },
        coral: {
          50: '#fff4f2',
          100: '#ffe5e0',
          200: '#ffcbc1',
          300: '#ffa496',
          400: '#ff7263',
          500: '#f74f3c',
          600: '#e23821',
          700: '#be2b18',
          800: '#9d2818',
          900: '#82261a',
          950: '#470f08',
        },
        ink: {
          50: '#f7f8fa',
          100: '#eef0f4',
          200: '#dfe3ea',
          300: '#c6ccd8',
          400: '#98a2b6',
          500: '#6b7689',
          600: '#4e586b',
          700: '#3b4354',
          800: '#252c39',
          900: '#151a23',
          950: '#0b0e14',
        },
        // Risk ramp, shared by badges, map markers and charts so one level always looks the same.
        risk: {
          low: '#10b981',
          medium: '#f59e0b',
          high: '#f97316',
          critical: '#e11d48',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
        display: ['Plus Jakarta Sans', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
      },
      letterSpacing: {
        tightest: '-0.035em',
      },
      boxShadow: {
        // Four steps, from "barely there" to "floating". Every surface picks one; nothing invents its own.
        hair: '0 1px 2px 0 rgb(17 30 73 / 0.04)',
        soft: '0 2px 6px -1px rgb(17 30 73 / 0.06), 0 1px 2px -1px rgb(17 30 73 / 0.04)',
        card: '0 10px 30px -12px rgb(17 30 73 / 0.14), 0 2px 6px -2px rgb(17 30 73 / 0.06)',
        lift: '0 26px 60px -24px rgb(17 30 73 / 0.28), 0 6px 14px -8px rgb(17 30 73 / 0.12)',
        glow: '0 20px 55px -22px rgb(47 90 230 / 0.55)',
      },
      backgroundImage: {
        'grid-faint':
          'linear-gradient(to right, rgb(17 30 73 / 0.05) 1px, transparent 1px), linear-gradient(to bottom, rgb(17 30 73 / 0.05) 1px, transparent 1px)',
        'brand-sheen': 'linear-gradient(135deg, #1c3479 0%, #2346c4 45%, #2f5ae6 100%)',
        'aurora':
          'radial-gradient(60% 55% at 12% 8%, rgb(47 203 187 / 0.20) 0%, transparent 60%), radial-gradient(55% 50% at 88% 4%, rgb(79 123 245 / 0.28) 0%, transparent 62%)',
      },
      backgroundSize: {
        grid: '44px 44px',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        'spin-slow': {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        'pulse-ring': {
          '0%': { transform: 'scale(0.7)', opacity: '0.55' },
          '70%': { transform: 'scale(1.9)', opacity: '0' },
          '100%': { transform: 'scale(1.9)', opacity: '0' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-420px 0' },
          '100%': { backgroundPosition: '420px 0' },
        },
      },
      animation: {
        'fade-in': 'fade-in 240ms cubic-bezier(0.22, 1, 0.36, 1)',
        float: 'float 6s ease-in-out infinite',
        'spin-slow': 'spin-slow 42s linear infinite',
        'pulse-ring': 'pulse-ring 3.2s cubic-bezier(0.22, 1, 0.36, 1) infinite',
        shimmer: 'shimmer 1.6s linear infinite',
      },
      transitionTimingFunction: {
        premium: 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
    },
  },
  plugins: [],
};
