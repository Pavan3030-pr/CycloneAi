import type { ImpactSummary } from '@/api/types';
import { cn } from '@/lib/cn';
import { formatDecimal } from '@/lib/format';
import { RISK_ORDER, RISK_STYLES } from '@/lib/risk';

/**
 * The headline numbers of an assessment.
 *
 * The four risk counts come first because they are the decision: how many assets need action. The
 * supporting figures (nearest approach, peak modelled wind) sit beneath as context rather than as
 * primary information.
 */
export function SummaryCards({ summary }: { summary: ImpactSummary }) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {RISK_ORDER.map((level) => {
          const style = RISK_STYLES[level];
          const count = summary.countByRisk[level] ?? 0;
          return (
            <div
              key={level}
              className="tile relative overflow-hidden"
              style={{ borderLeft: `3px solid ${style.hex}` }}
            >
              <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {style.label}
              </p>
              <p className="mt-1 text-3xl font-semibold tabular-nums text-slate-900 dark:text-slate-50">{count}</p>
              <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                {count === 1 ? 'asset' : 'assets'}
              </p>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Metric label="Assets assessed" value={String(summary.totalAssets)} />
        <Metric label="Nearest approach" value={`${formatDecimal(summary.nearestDistanceNauticalMiles)} nm`} />
        <Metric label="Peak modelled wind" value={`${summary.peakEstimatedWindKnots} kt`} />
      </div>
    </div>
  );
}

function Metric({ label, value, className }: { label: string; value: string; className?: string }) {
  return (
    <div className={cn('tile', className)}>
      <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500 dark:text-slate-400">{label}</p>
      <p className="mt-1 text-lg font-semibold tabular-nums text-slate-900 dark:text-slate-100">{value}</p>
    </div>
  );
}
