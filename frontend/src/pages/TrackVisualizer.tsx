import { Map as MapIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { RiskBadge } from '@/components/RiskBadge';
import { MapLegend, TrackMap } from '@/components/TrackMap';
import { Button, Card, EmptyState, PageHeader } from '@/components/ui';
import { formatDecimal, formatInstant } from '@/lib/format';
import { ASSET_TYPE_LABELS } from '@/lib/risk';
import { useActiveScenario } from '@/store/scenarioStore';

/**
 * Spatially explicit view of one storm, with the timeline that produced it.
 *
 * The fix table and the map sit together deliberately: the map shows where the storm is, the table
 * shows why, since each row is a published fix with the wind and pressure the model interpolated
 * between. The interpolated centre is marked separately from the fixes so nobody mistakes a computed
 * position for a published one.
 */
export function TrackVisualizer() {
  const scenario = useActiveScenario();

  if (scenario === null) {
    return (
      <>
        <PageHeader eyebrow="Spatial" title="Track visualizer" />
        <Card>
          <EmptyState
            icon={<MapIcon className="size-5" />}
            title="Nothing to draw yet"
            description="Run an assessment — or load the demonstration storm from the dashboard — and the track will be plotted here."
            action={
              <Link to="/app/assessment">
                <Button>Go to impact assessment</Button>
              </Link>
            }
          />
        </Card>
      </>
    );
  }

  const { assessment, points, assets } = scenario;
  const orderedPoints = [...points].sort((left, right) => left.timestamp.localeCompare(right.timestamp));
  const spanHours =
    orderedPoints.length > 1
      ? (Date.parse(orderedPoints[orderedPoints.length - 1].timestamp) - Date.parse(orderedPoints[0].timestamp)) / 3_600_000
      : 0;

  return (
    <>
      <PageHeader
        eyebrow="Spatial"
        title={`${scenario.stormId} track`}
        description={`${orderedPoints.length} published fixes spanning ${spanHours.toFixed(0)} hours, with the interpolated centre at ${formatInstant(assessment.evaluatedAt)}.`}
        actions={
          <Link to="/app/assessment">
            <Button variant="secondary">Re-assess this track</Button>
          </Link>
        }
      />

      <div className="space-y-5">
        <Card title="Map" subtitle="Dashed rings mark the 60 nm and 120 nm screening bands">
          <TrackMap
            points={points}
            stormPosition={assessment.stormPosition}
            exposures={assessment.exposures}
            assets={assets}
            heightClass="h-[32rem]"
          />
          <div className="mt-3.5">
            <MapLegend />
          </div>
        </Card>

        <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
          <Card title="Published fixes" subtitle="Sorted by valid time, as the track aggregate stores them">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[26rem] border-collapse text-sm">
                <thead>
                  <tr className="text-left text-[11px] uppercase tracking-[0.12em] text-ink-500">
                    <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Valid time</th>
                    <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Position</th>
                    <th className="border-b border-ink-200 pb-2.5 pr-3 text-right font-semibold">Wind</th>
                    <th className="border-b border-ink-200 pb-2.5 text-right font-semibold">Pressure</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {orderedPoints.map((point) => (
                    <tr key={point.timestamp} className="transition hover:bg-ink-50/70">
                      <td className="py-2.5 pr-3 text-ink-700">{formatInstant(point.timestamp)}</td>
                      <td className="py-2.5 pr-3 font-mono text-xs text-ink-600">
                        {formatDecimal(point.latitude, 2)}, {formatDecimal(point.longitude, 2)}
                      </td>
                      <td className="py-2.5 pr-3 text-right font-mono tabular-nums text-ink-700">
                        {point.windSpeedKnots} kt
                      </td>
                      <td className="py-2.5 text-right font-mono tabular-nums text-ink-700">
                        {point.centralPressureMb} mb
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          <Card title="Assessed assets" subtitle={`${assets.length} assets submitted with this track`}>
            <ul className="space-y-3.5">
              {assets.map((asset) => {
                const exposure = assessment.exposures.find((candidate) => candidate.assetId === asset.id);
                return (
                  <li
                    key={asset.id}
                    className="flex items-start justify-between gap-3 border-b border-ink-100 pb-3.5 last:border-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-ink-900">{asset.name}</p>
                      <p className="mt-0.5 text-xs text-ink-500">
                        {ASSET_TYPE_LABELS[asset.assetType]} · {formatDecimal(asset.latitude, 2)},{' '}
                        {formatDecimal(asset.longitude, 2)}
                      </p>
                      {exposure !== undefined ? (
                        <p className="mt-1 font-mono text-[11px] text-ink-500">
                          {formatDecimal(exposure.distanceNauticalMiles)} nm · {exposure.estimatedWindAtAsset} kt
                        </p>
                      ) : null}
                    </div>
                    {exposure !== undefined ? <RiskBadge level={exposure.riskLevel} /> : null}
                  </li>
                );
              })}
            </ul>
          </Card>
        </div>
      </div>
    </>
  );
}
