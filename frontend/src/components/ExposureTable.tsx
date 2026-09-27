import type { AssetExposure } from '@/api/types';
import { formatDecimal } from '@/lib/format';
import { ASSET_TYPE_LABELS, ASSET_TYPE_SHORT, RISK_STYLES } from '@/lib/risk';
import { RiskBadge } from './RiskBadge';

/**
 * Per-asset results, in the order the API returns them (most severe, then nearest).
 *
 * The rationale column is the point of this table: it shows which screening criterion fired, so a
 * reader can disagree with a specific rule instead of having to trust a number. Two layouts are
 * rendered from the same data — a table where there is room for six columns, and a stacked card list
 * on a phone — because a horizontally scrolling results table is unreadable exactly when the reader
 * is standing on a beach.
 */
export function ExposureTable({ exposures }: { exposures: AssetExposure[] }) {
  if (exposures.length === 0) {
    return <p className="text-sm text-ink-500">No assets were assessed.</p>;
  }

  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[52rem] border-collapse text-sm">
          <thead>
            <tr className="text-left text-[11px] uppercase tracking-[0.12em] text-ink-500">
              <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Asset</th>
              <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Type</th>
              <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Risk</th>
              <th className="border-b border-ink-200 pb-2.5 pr-3 text-right font-semibold">Distance</th>
              <th className="border-b border-ink-200 pb-2.5 pr-3 text-right font-semibold">Modelled wind</th>
              <th className="border-b border-ink-200 pb-2.5 font-semibold">Basis</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {exposures.map((exposure) => (
              <tr key={exposure.assetId} className="align-top transition hover:bg-ink-50/70">
                <td className="py-3 pr-3">
                  <p className="font-semibold text-ink-900">{exposure.assetName}</p>
                  <p className="font-mono text-[11px] text-ink-500">{exposure.assetId}</p>
                </td>
                <td className="py-3 pr-3 text-ink-600">{ASSET_TYPE_LABELS[exposure.assetType]}</td>
                <td className="py-3 pr-3">
                  <RiskBadge level={exposure.riskLevel} />
                </td>
                <td className="py-3 pr-3 text-right font-mono tabular-nums text-ink-700">
                  {formatDecimal(exposure.distanceNauticalMiles)} nm
                </td>
                <td className="py-3 pr-3 text-right font-mono tabular-nums text-ink-700">
                  {exposure.estimatedWindAtAsset} kt
                </td>
                <td className="max-w-sm py-3 text-xs leading-relaxed text-ink-500">{exposure.rationale}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ul className="space-y-3 md:hidden">
        {exposures.map((exposure) => (
          <li
            key={exposure.assetId}
            className="rounded-xl border border-ink-200/80 p-4 shadow-hair"
            style={{ borderLeft: `3px solid ${RISK_STYLES[exposure.riskLevel].hex}` }}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate font-semibold text-ink-900">{exposure.assetName}</p>
                <p className="font-mono text-[11px] text-ink-500">
                  {ASSET_TYPE_SHORT[exposure.assetType]} · {exposure.assetId}
                </p>
              </div>
              <RiskBadge level={exposure.riskLevel} />
            </div>
            <div className="mt-3 flex gap-5 font-mono text-xs text-ink-700">
              <span>{formatDecimal(exposure.distanceNauticalMiles)} nm</span>
              <span>{exposure.estimatedWindAtAsset} kt</span>
            </div>
            <p className="mt-2.5 text-xs leading-relaxed text-ink-500">{exposure.rationale}</p>
          </li>
        ))}
      </ul>
    </>
  );
}
