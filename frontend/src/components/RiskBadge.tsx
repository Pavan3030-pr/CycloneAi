import type { RiskLevel, SaffirSimpsonCategory } from '@/api/types';
import { cn } from '@/lib/cn';
import { CATEGORY_STYLES, RISK_STYLES } from '@/lib/risk';

export function RiskBadge({ level, className }: { level: RiskLevel; className?: string }) {
  const style = RISK_STYLES[level];
  return (
    <span className={cn('badge', style.badge, className)}>
      <span className={cn('size-1.5 rounded-full', style.dot)} />
      {style.label}
    </span>
  );
}

export function CategoryBadge({ category }: { category: SaffirSimpsonCategory }) {
  return <span className={cn('badge', CATEGORY_STYLES[category] ?? CATEGORY_STYLES.TS)}>{category}</span>;
}
