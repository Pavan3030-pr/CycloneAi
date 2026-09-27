import { Link } from 'react-router-dom';
import { RiskBadge } from '@/components/RiskBadge';
import { MapLegend, TrackMap } from '@/components/TrackMap';
import { Button, Card, EmptyState } from '@/components/ui';
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
      <Card title="Track visualizer">
        <EmptyState
          title="Nothing to draw yet"
          description="Load the demonstration storm from the sidebar, or run an assessment, and the track will be plotted here."
          action={
            <Link to="/assessment">
              <Button>Go to impact assessment</Button>
            </Link>
          }
        />
      </Card>
    );
  }

  const { assessment, points, assets } = scenario;
  const orderedPoints = [...points].sort((left, right) => left.timestamp.localeCompare(right.timestamp));

  return (
    <div className="space-y-5">
      <Card
        title={`${scenario.stormId} track`}
        subtitle={`${orderedPoints.length} published fixes · interpolated centre at ${formatInstant(assessment.evaluatedAt)}`}
      >
        <TrackMap
          points={points}
          stormPosition={assessment.stormPosition}
          exposures={assessment.exposures}
          assets={assets}
          heightClass="h-[32rem]"
        />
        <div className="mt-3">
          <MapLegend />
        </div>
      </Card>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <Card title="Published fixes" subtitle="Sorted by valid time, as the aggregate stores them">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[26rem] border-collapse text-sm">
              <thead>
                <tr className="text-left text-[11px] uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  <th className="pb-2 pr-3 font-medium">Valid time</th>
                  <th className="pb-2 pr-3 font-medium">Position</th>
                  <th className="pb-2 pr-3 text-right font-medium">Wind</th>
                  <th className="pb-2 text-right font-medium">Pressure</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {orderedPoints.map((point) => (
                  <tr key={point.timestamp}>
                    <td className="py-2 pr-3 text-slate-700 dark:text-slate-200">{formatInstant(point.timestamp)}</td>
                    <td className="py-2 pr-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {formatDecimal(point.latitude, 2)}, {formatDecimal(point.longitude, 2)}
                    </td>
                    <td className="py-2 pr-3 text-right tabular-nums text-slate-700 dark:text-slate-200">
                      {point.windSpeedKnots} kt
                    </td>
                    <td className="py-2 text-right tabular-nums text-slate-700 dark:text-slate-200">
                      {point.centralPressureMb} mb
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card
          title="Assessed assets"
          subtitle={`${assets.length} assets submitted with this track`}
          actions={<Link to="/assessment"><Button variant="secondary">Re-assess</Button></Link>}
        >
          <ul className="space-y-3">
            {assets.map((asset) => {
              const exposure = assessment.exposures.find((candidate) => candidate.assetId === asset.id);
              return (
                <li key={asset.id} className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-slate-800 dark:text-slate-100">{asset.name}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {ASSET_TYPE_LABELS[asset.assetType]} · {formatDecimal(asset.latitude, 2)},{' '}
                      {formatDecimal(asset.longitude, 2)}
                    </p>
                    {exposure !== undefined ? (
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
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
  );
}
