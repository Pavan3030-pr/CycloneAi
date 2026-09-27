import { Link } from 'react-router-dom';
import { AdvisoryPanel } from '@/components/AdvisoryPanel';
import { CategoryBadge, RiskBadge } from '@/components/RiskBadge';
import { Button, Card, EmptyState } from '@/components/ui';
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
      <Card title="Advisory panel">
        <EmptyState
          title="No advisory generated yet"
          description="Run an assessment or load the demonstration storm, and the generated text appears here."
          action={
            <Link to="/assessment">
              <Button>Run an assessment</Button>
            </Link>
          }
        />
      </Card>
    );
  }

  const { assessment } = scenario;

  return (
    <div className="space-y-5">
      <Card
        title={`Advisory for ${scenario.stormId}`}
        subtitle={`Generated for the assessment valid at ${formatInstant(assessment.evaluatedAt)}`}
        actions={
          <>
            <CategoryBadge category={assessment.stormPosition.category} />
            <RiskBadge level={assessment.summary.highestRisk} />
          </>
        }
      >
        <AdvisoryPanel assessment={assessment} />
      </Card>

      <Card title="How to read this" subtitle="What the text is built from">
        <ul className="list-inside list-disc space-y-1.5 text-sm text-slate-600 dark:text-slate-300">
          <li>The storm centre is interpolated between the published fixes, so it is a modelled position, not an observation.</li>
          <li>
            Modelled wind at an asset is the storm&apos;s maximum sustained wind decayed exponentially with distance, using a
            75 nm e-folding scale.
          </li>
          <li>Risk level is the more severe of the wind threshold and the distance band, and the reason is stated per asset.</li>
          <li>Terrain, gust factor, quadrant asymmetry and forecast positional uncertainty are not modelled.</li>
        </ul>
      </Card>
    </div>
  );
}
