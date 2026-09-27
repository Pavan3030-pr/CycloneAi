import { useCallback, useState } from 'react';
import { useAssessImpact, useRegisterAsset } from '@/api/hooks';
import { useAuth } from '@/auth/AuthProvider';
import { publishScenario } from '@/store/scenarioStore';
import { bayOfBengalScenario, demoAssessmentInput } from './demoScenario';

/**
 * Drives the one-click demonstration end to end.
 *
 * It signs in with the configured demo account if the session is missing, registers the sample
 * assets in the real registry, runs the real assessment endpoint and publishes the result to the
 * screen. Nothing is stubbed: the numbers a viewer sees came out of the exposure model on the
 * server. Asset registration is best-effort, because a demo account that is only a VIEWER can
 * legitimately assess but not write, and that should not block the demonstration.
 */
export function useRunDemo() {
  const { isAuthenticated, demoLogin } = useAuth();
  const registerAsset = useRegisterAsset();
  const assessImpact = useAssessImpact();
  const [error, setError] = useState<unknown>(null);

  const run = useCallback(async () => {
    setError(null);
    try {
      if (!isAuthenticated) {
        await demoLogin();
      }
      const scenario = bayOfBengalScenario;
      await Promise.allSettled(scenario.assets.map((asset) => registerAsset.mutateAsync(asset)));
      const assessment = await assessImpact.mutateAsync(demoAssessmentInput(scenario));
      publishScenario({
        stormId: scenario.stormId,
        points: scenario.points,
        assets: scenario.assets,
        assessment,
      });
      return assessment;
    } catch (cause) {
      setError(cause);
      throw cause;
    }
  }, [assessImpact, demoLogin, isAuthenticated, registerAsset]);

  return { run, isRunning: assessImpact.isPending || registerAsset.isPending, error };
}
