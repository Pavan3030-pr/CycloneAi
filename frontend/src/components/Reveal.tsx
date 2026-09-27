import { motion, useReducedMotion, type Variants } from 'framer-motion';
import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

/**
 * Scroll-triggered entrance animation.
 *
 * One wrapper for the whole site, so every section arrives with the same timing and nobody is tempted
 * to hand-roll a different duration per card. `useReducedMotion` collapses the animation to a plain
 * fade for anyone who has asked the operating system for less movement.
 */

const DISTANCE = 18;

interface RevealProps {
  children: ReactNode;
  className?: string;
  /** Stagger, in seconds, for items revealed in sequence. */
  delay?: number;
  as?: 'div' | 'section' | 'li' | 'article';
}

export function Reveal({ children, className, delay = 0, as = 'div' }: RevealProps) {
  const reduceMotion = useReducedMotion();
  const MotionTag = motion[as];

  return (
    <MotionTag
      className={cn(className)}
      initial={{ opacity: 0, y: reduceMotion ? 0 : DISTANCE }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.55, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </MotionTag>
  );
}

/** Shared container/item variants for staggered grids such as the feature cards. */
export const staggerContainer: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.09, delayChildren: 0.05 } },
};

export const staggerItem: Variants = {
  hidden: { opacity: 0, y: DISTANCE },
  visible: { opacity: 1, y: 0, transition: { duration: 0.55, ease: [0.22, 1, 0.36, 1] } },
};
