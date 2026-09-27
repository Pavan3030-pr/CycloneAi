import { Activity, Gauge, MapPin, Radar, Sparkles, Wind } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { AssetExposure } from '@/api/types';
import { AdvisoryPreview } from '@/components/AdvisoryPreview';
import { ExposureTable } from '@/components/ExposureTable';
import { CategoryBadge, RiskBadge } from '@/components/RiskBadge';
import { SummaryCards } from '@/components/SummaryCards';
import { MapLegend, TrackMap } from '@/components/TrackMap';
import { Button, Card, EmptyState, ErrorNote, PageHeader } from '@/components/ui';
import { bayOfBengalScenario } from '@/demo/demoScenario';
import { useRunDemo } from '@/demo/useRunDemo';
import { formatDecimal, formatInstant } from '@/lib/format';
import { RISK_STYLES } from '@/lib/risk';
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
      <>
        <PageHeader
          eyebrow="Overview"
          title="Dashboard"
          description="The console reads from the last assessment you ran against the live API."
        />
        <div className="mx-auto max-w-3xl">
          <div className="card p-6 sm:p-8">
            <EmptyState
              icon={<Radar className="size-5" />}
              title="Load the demonstration storm"
              description={`A synthetic six-hourly track from the southern Bay of Bengal to landfall near Bapatla, assessed against ${bayOfBengalScenario.assets.length} coastal assets through the live exposure model.`}
              action={
                <>
                  <Button onClick={runDemo} busy={demo.isRunning}>
                    {demo.isRunning ? null : <Sparkles className="size-4" />}
                    {demo.isRunning ? 'Running the demo storm…' : 'Load demonstration storm'}
                  </Button>
                  <Link to="/app/assessment">
                    <Button variant="secondary">Run your own assessment</Button>
                  </Link>
                </>
              }
            />
            <ErrorNote error={error ?? demo.error} className="mt-5" />
          </div>
        </div>
      </>
    );
  }

  const { assessment, points, assets } = scenario;
  const actionables = assessment.exposures.filter(isActionable);
  const highest = RISK_STYLES[assessment.summary.highestRisk];

  return (
    <>
      <PageHeader
        eyebrow="Overview"
        title={`Storm ${scenario.stormId}`}
        description={`Assessed against ${assessment.summary.totalAssets} assets at ${formatInstant(assessment.evaluatedAt)}. Highest assessed level: ${highest.label}.`}
        actions={
          <>
            <CategoryBadge category={assessment.stormPosition.category} />
            <RiskBadge level={assessment.summary.highestRisk} />
            <Button variant="secondary" onClick={runDemo} busy={demo.isRunning}>
              {demo.isRunning ? 'Running…' : 'Re-run demo'}
            </Button>
          </>
        }
      />

      <div className="space-y-5">
        <ErrorNote error={error ?? demo.error} />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            icon={<MapPin className="size-4" />}
            label="Interpolated centre"
            value={`${formatDecimal(assessment.stormPosition.latitude, 2)}°, ${formatDecimal(assessment.stormPosition.longitude, 2)}°`}
          />
          <MetricCard
            icon={<Wind className="size-4" />}
            label="Max sustained wind"
            value={`${assessment.stormPosition.windSpeedKnots} kt`}
          />
          <MetricCard
            icon={<Gauge className="size-4" />}
            label="Central pressure"
            value={`${assessment.stormPosition.centralPressureMb} mb`}
          />
          <MetricCard
            icon={<Activity className="size-4" />}
            label="GeoJSON position"
            value={assessment.stormPosition.coordinateString}
            mono
          />
        </div>

        <SummaryCards summary={assessment.summary} />

        <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
          <Card
            title="Track and assets"
            subtitle="Markers coloured by assessed risk; dashed rings are the screening bands"
            className="xl:col-span-2"
          >
            <TrackMap
              points={points}
              stormPosition={assessment.stormPosition}
              exposures={assessment.exposures}
              assets={assets}
              heightClass="h-[24rem]"
            />
            <div className="mt-3.5">
              <MapLegend />
            </div>
          </Card>

          <div className="space-y-5">
            <Card
              title="Requiring action"
              subtitle={`${actionables.length} of ${assessment.summary.totalAssets} assets at high or critical risk`}
            >
              {actionables.length === 0 ? (
                <p className="text-sm text-ink-500">
                  No asset reached high or critical risk at this evaluation time.
                </p>
              ) : (
                <ul className="space-y-3">
                  {actionables.slice(0, 6).map((exposure) => (
                    <li
                      key={exposure.assetId}
                      className="flex items-start justify-between gap-3 border-b border-ink-100 pb-3 last:border-0 last:pb-0"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-ink-900">{exposure.assetName}</p>
                        <p className="mt-0.5 font-mono text-[11px] text-ink-500">
                          {formatDecimal(exposure.distanceNauticalMiles)} nm · {exposure.estimatedWindAtAsset} kt
                        </p>
                        <p className="mt-1 text-[11px] text-ink-500">
                          {RISK_STYLES[exposure.riskLevel].action}
                        </p>
                      </div>
                      <RiskBadge level={exposure.riskLevel} />
                    </li>
                  ))}
                </ul>
              )}
              <div className="mt-4">
                <Link to="/app/advisory">
                  <Button variant="secondary" className="w-full">
                    Open advisory
                  </Button>
                </Link>
              </div>
            </Card>

            <AdvisoryPreview advisory={assessment.advisory} />
          </div>
        </div>

        <Card title="Every assessed asset" subtitle="Ordered by severity, then by distance from the centre">
          <ExposureTable exposures={assessment.exposures} />
        </Card>
      </div>
    </>
  );
}

function isActionable(exposure: AssetExposure): boolean {
  return exposure.riskLevel === 'HIGH' || exposure.riskLevel === 'CRITICAL';
}

function MetricCard({
  icon,
  label,
  value,
  mono = false,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="card p-4">
      <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
        <span className="text-brand-600">{icon}</span>
        {label}
      </div>
      <p className={mono ? 'mt-2 font-mono text-sm font-semibold text-ink-900' : 'mt-2 font-display text-lg font-bold tracking-tight text-ink-900'}>
        {value}
      </p>
    </div>
  );
}
