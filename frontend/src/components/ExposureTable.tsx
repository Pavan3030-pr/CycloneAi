import type { AssetExposure } from '@/api/types';
import { formatDecimal } from '@/lib/format';
import { ASSET_TYPE_LABELS } from '@/lib/risk';
import { RiskBadge } from './RiskBadge';

/**
 * Per-asset results, in the order the API returns them (most severe, then nearest).
 *
 * The rationale column is the point of this table: it shows which screening criterion fired, so a
 * reader can disagree with a specific rule instead of having to trust a number.
 */
export function ExposureTable({ exposures }: { exposures: AssetExposure[] }) {
  if (exposures.length === 0) {
    return <p className="text-sm text-slate-500 dark:text-slate-400">No assets were assessed.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[46rem] border-collapse text-sm">
        <thead>
          <tr className="text-left text-[11px] uppercase tracking-wider text-slate-500 dark:text-slate-400">
            <th className="pb-2 pr-3 font-medium">Asset</th>
            <th className="pb-2 pr-3 font-medium">Type</th>
            <th className="pb-2 pr-3 font-medium">Risk</th>
            <th className="pb-2 pr-3 text-right font-medium">Distance</th>
            <th className="pb-2 pr-3 text-right font-medium">Modelled wind</th>
            <th className="pb-2 font-medium">Basis</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
          {exposures.map((exposure) => (
            <tr key={exposure.assetId} className="align-top">
              <td className="py-2.5 pr-3">
                <p className="font-medium text-slate-800 dark:text-slate-100">{exposure.assetName}</p>
                <p className="font-mono text-[11px] text-slate-500 dark:text-slate-400">{exposure.assetId}</p>
              </td>
              <td className="py-2.5 pr-3 text-slate-600 dark:text-slate-300">
                {ASSET_TYPE_LABELS[exposure.assetType]}
              </td>
              <td className="py-2.5 pr-3">
                <RiskBadge level={exposure.riskLevel} />
              </td>
              <td className="py-2.5 pr-3 text-right tabular-nums text-slate-700 dark:text-slate-200">
                {formatDecimal(exposure.distanceNauticalMiles)} nm
              </td>
              <td className="py-2.5 pr-3 text-right tabular-nums text-slate-700 dark:text-slate-200">
                {exposure.estimatedWindAtAsset} kt
              </td>
              <td className="max-w-sm py-2.5 text-xs text-slate-500 dark:text-slate-400">{exposure.rationale}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
