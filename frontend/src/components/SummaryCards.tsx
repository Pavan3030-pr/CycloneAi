import type { ImpactSummary } from '@/api/types';
import { formatDecimal } from '@/lib/format';
import { RISK_ORDER, RISK_STYLES } from '@/lib/risk';

/**
 * The headline numbers of an assessment.
 *
 * The four risk counts come first because they are the decision: how many assets need action. A
 * distribution bar sits under them so the shape of the exposure is visible without reading four
 * numbers, and the supporting figures (nearest approach, peak modelled wind) sit beneath as context
 * rather than as primary information.
 */
export function SummaryCards({ summary }: { summary: ImpactSummary }) {
  const total = RISK_ORDER.reduce((sum, level) => sum + (summary.countByRisk[level] ?? 0), 0);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {RISK_ORDER.map((level) => {
          const style = RISK_STYLES[level];
          const count = summary.countByRisk[level] ?? 0;
          return (
            <div key={level} className={`rounded-xl border p-4 shadow-hair ${style.tile}`}>
              <div className="flex items-center gap-2">
                <span className={`size-2 rounded-full ${style.dot}`} />
                <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-600">{style.label}</p>
              </div>
              <p className="mt-2 font-display text-3xl font-extrabold tabular-nums tracking-tight text-ink-900">
                {count}
              </p>
              <p className="mt-0.5 text-[11px] text-ink-600">{count === 1 ? 'asset' : 'assets'}</p>
            </div>
          );
        })}
      </div>

      <div className="card p-4">
        <div className="flex items-center justify-between text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
          <span>Exposure distribution</span>
          <span className="font-mono normal-case tracking-normal">
            {total} {total === 1 ? 'asset' : 'assets'} assessed
          </span>
        </div>
        <div className="mt-3 flex h-2.5 w-full overflow-hidden rounded-full bg-ink-100">
          {RISK_ORDER.map((level) => {
            const count = summary.countByRisk[level] ?? 0;
            if (count === 0 || total === 0) {
              return null;
            }
            return (
              <div
                key={level}
                title={`${RISK_STYLES[level].label}: ${count}`}
                style={{ width: `${(count / total) * 100}%`, background: RISK_STYLES[level].hex }}
                className="h-full transition-all duration-500 ease-premium"
              />
            );
          })}
        </div>
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <Metric label="Assets assessed" value={String(summary.totalAssets)} />
          <Metric label="Nearest approach" value={`${formatDecimal(summary.nearestDistanceNauticalMiles)} nm`} />
          <Metric label="Peak modelled wind" value={`${summary.peakEstimatedWindKnots} kt`} />
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-ink-200/70 bg-ink-50/60 px-3.5 py-2.5">
      <p className="text-[11px] font-medium uppercase tracking-[0.1em] text-ink-500">{label}</p>
      <p className="mt-0.5 font-display text-base font-bold tabular-nums text-ink-900">{value}</p>
    </div>
  );
}
