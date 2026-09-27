import type { RiskLevel, SaffirSimpsonCategory } from '@/api/types';
import { cn } from '@/lib/cn';
import { CATEGORY_STYLES, RISK_STYLES } from '@/lib/risk';

export function RiskBadge({
  level,
  className,
  showDot = true,
}: {
  level: RiskLevel;
  className?: string;
  showDot?: boolean;
}) {
  const style = RISK_STYLES[level];
  return (
    <span className={cn('badge', style.badge, className)}>
      {showDot ? <span className={cn('size-1.5 rounded-full', style.dot)} /> : null}
      {style.label}
    </span>
  );
}

export function CategoryBadge({ category, className }: { category: SaffirSimpsonCategory; className?: string }) {
  return (
    <span className={cn('badge font-mono', CATEGORY_STYLES[category] ?? CATEGORY_STYLES.TS, className)}>
      {category}
    </span>
  );
}
