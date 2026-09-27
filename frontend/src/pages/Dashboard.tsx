import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { AssetExposure } from '@/api/types';
import { AdvisoryPreview } from '@/components/AdvisoryPreview';
import { ExposureTable } from '@/components/ExposureTable';
import { CategoryBadge, RiskBadge } from '@/components/RiskBadge';
import { SummaryCards } from '@/components/SummaryCards';
import { TrackMap, MapLegend } from '@/components/TrackMap';
import { Button, Card, EmptyState, ErrorNote } from '@/components/ui';
import { bayOfBengalScenario } from '@/demo/demoScenario';
import { useRunDemo } from '@/demo/useRunDemo';
import { formatDecimal, formatInstant } from '@/lib/format';
import { useActiveScenario } from '@/store/scenarioStore';

/**
 * Overview of the storm currently under assessment.
 *
 * Reads only from the active scenario, so it never shows numbers that were not computed for the
 * track on screen. With no scenario loaded it explains how to load one rather than rendering empty
 * tiles, and the demo button is right there because that is the fastest honest path to a full view.
 */
export function Dashboard() {
  const scenario = useActiveScenario();
  const demo = useRunDemo();
  const [error, setError] = useState<unknown>(null);

  const runDemo = async () => {
    setError(null);
    try {
      await demo.run();
    } catch (cause) {
      setError(cause);
    }
  };

  if (scenario === null) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Card title="No storm under assessment yet" subtitle="The console reads from the last assessment you ran">
          <EmptyState
            title="Load the demonstration storm"
            description={`A synthetic six-hourly track from the southern Bay of Bengal to landfall near Bapatla, assessed against ${bayOfBengalScenario.assets.length} coastal assets through the live API.`}
            action={
              <Button onClick={runDemo} busy={demo.isRunning}>
                Load demonstration storm
              </Button>
            }
          />
          <div className="mt-4 text-center text-xs text-slate-500 dark:text-slate-400">
            or{' '}
            <Link className="font-medium text-sky-600 underline-offset-2 hover:underline dark:text-sky-400" to="/assessment">
              run your own assessment
            </Link>
          </div>
          {error !== null ? <div className="mt-4"><ErrorNote error={error} /></div> : null}
        </Card>
      </div>
    );
  }

  const { assessment, points, assets } = scenario;
  const actionables = assessment.exposures.filter((exposure) => isActionable(exposure));

  return (
    <div className="space-y-5">
      <Card
        title={`Storm ${scenario.stormId}`}
        subtitle={`Evaluated at ${formatInstant(assessment.evaluatedAt)}`}
        actions={
          <>
            <CategoryBadge category={assessment.stormPosition.category} />
            <RiskBadge level={assessment.summary.highestRisk} />
          </>
        }
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Metric label="Interpolated centre" value={`${formatDecimal(assessment.stormPosition.latitude, 2)}°, ${formatDecimal(assessment.stormPosition.longitude, 2)}°`} />
          <Metric label="Max sustained wind" value={`${assessment.stormPosition.windSpeedKnots} kt`} />
          <Metric label="Central pressure" value={`${assessment.stormPosition.centralPressureMb} mb`} />
          <Metric label="GeoJSON position" value={assessment.stormPosition.coordinateString} mono />
        </div>
      </Card>

      <SummaryCards summary={assessment.summary} />

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card title="Track and assets" subtitle="Markers coloured by assessed risk" className="xl:col-span-2">
          <TrackMap
            points={points}
            stormPosition={assessment.stormPosition}
            exposures={assessment.exposures}
            assets={assets}
            heightClass="h-[24rem]"
          />
          <div className="mt-3">
            <MapLegend />
          </div>
        </Card>

        <div className="space-y-5">
          <Card
            title="Requiring action"
            subtitle={`${actionables.length} of ${assessment.summary.totalAssets} assets at high or critical risk`}
          >
            {actionables.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">
                No asset reached high or critical risk at this evaluation time.
              </p>
            ) : (
              <ul className="space-y-2.5">
                {actionables.slice(0, 6).map((exposure) => (
                  <li key={exposure.assetId} className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-slate-800 dark:text-slate-100">
                        {exposure.assetName}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {formatDecimal(exposure.distanceNauticalMiles)} nm · {exposure.estimatedWindAtAsset} kt
                      </p>
                    </div>
                    <RiskBadge level={exposure.riskLevel} />
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-4">
              <Link to="/advisory">
                <Button variant="secondary" className="w-full">
                  Open advisory
                </Button>
              </Link>
            </div>
          </Card>

          <AdvisoryPreview advisory={assessment.advisory} />
        </div>
      </div>

      <Card title="Every assessed asset" subtitle="Ordered by severity, then distance">
        <ExposureTable exposures={assessment.exposures} />
      </Card>
    </div>
  );
}

function isActionable(exposure: AssetExposure): boolean {
  return exposure.riskLevel === 'HIGH' || exposure.riskLevel === 'CRITICAL';
}

function Metric({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500 dark:text-slate-400">{label}</p>
      <p className={mono === true ? 'mt-1 font-mono text-sm text-slate-800 dark:text-slate-100' : 'mt-1 text-sm font-medium text-slate-800 dark:text-slate-100'}>
        {value}
      </p>
    </div>
  );
}
