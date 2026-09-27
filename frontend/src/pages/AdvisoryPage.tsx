import { BellRing } from 'lucide-react';
import { AdvisoryPanel } from '@/components/AdvisoryPanel';
import { CategoryBadge, RiskBadge } from '@/components/RiskBadge';
import { Card, EmptyState, PageHeader } from '@/components/ui';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';
import { formatInstant } from '@/lib/format';
import { useActiveScenario } from '@/store/scenarioStore';

/**
 * The advisory as a standalone artefact.
 *
 * Kept on its own screen because advisories get read aloud and pasted into other systems, so the
 * text should not compete with tables and maps. The header states which storm and instant the text
 * belongs to, since an advisory without its validity time is worse than no advisory.
 */
export function AdvisoryPage() {
  const scenario = useActiveScenario();

  if (scenario === null) {
    return (
      <>
        <PageHeader eyebrow="Output" title="Advisory panel" />
        <Card>
          <EmptyState
            icon={<BellRing className="size-5" />}
            title="No advisory generated yet"
            description="Run an assessment — or load the demonstration storm — and the generated early-warning text appears here."
            action={
              <Link to="/app/assessment">
                <Button>Run an assessment</Button>
              </Link>
            }
          />
        </Card>
      </>
    );
  }

  const { assessment } = scenario;

  return (
    <>
      <PageHeader
        eyebrow="Output"
        title={`Advisory for ${scenario.stormId}`}
        description={`Generated for the assessment valid at ${formatInstant(assessment.evaluatedAt)}, and published alongside the exposure table it was built from.`}
        actions={
          <>
            <CategoryBadge category={assessment.stormPosition.category} />
            <RiskBadge level={assessment.summary.highestRisk} />
          </>
        }
      />

      <div className="space-y-5">
        <Card title="Generated text" subtitle="Deterministic: the same assessment always produces the same advisory">
          <AdvisoryPanel assessment={assessment} />
        </Card>

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          <Card title="How to read this" subtitle="What the text is built from">
            <ul className="space-y-2.5 text-sm text-ink-600">
              {[
                'The storm centre is interpolated between published fixes, so it is a modelled position, not an observation.',
                'Modelled wind at an asset is the storm\u2019s maximum sustained wind decayed exponentially with distance, using a 75 nm e-folding scale.',
                'Risk level is the more severe of the wind threshold and the distance band, and the reason is stated per asset.',
                'Terrain, gust factor, quadrant asymmetry and forecast positional uncertainty are not modelled.',
              ].map((line) => (
                <li key={line} className="flex gap-2.5">
                  <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-brand-500" />
                  <span className="leading-relaxed">{line}</span>
                </li>
              ))}
            </ul>
          </Card>

          <Card title="Assets named in this briefing" subtitle="In the order the advisory lists them">
            {assessment.exposures.length === 0 ? (
              <p className="text-sm text-ink-500">No assets were assessed for this storm.</p>
            ) : (
              <ul className="space-y-3">
                {assessment.exposures.map((exposure) => (
                  <li
                    key={exposure.assetId}
                    className="flex items-center justify-between gap-3 border-b border-ink-100 pb-3 last:border-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink-900">{exposure.assetName}</p>
                      <p className="font-mono text-[11px] text-ink-500">{exposure.assetId}</p>
                    </div>
                    <RiskBadge level={exposure.riskLevel} />
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>
    </>
  );
}
