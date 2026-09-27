import {
  BadgeCheck,
  Check,
  Copy,
  Download,
  FileCode2,
  Languages,
  Loader2,
  Radio,
  Send,
  TriangleAlert,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { useAssessImpact, useAdvisoryChannel, useDispatchAdvisory } from '@/api/hooks';
import { downloadCapAlert } from '@/api/client';
import type { AdvisoryDispatch, AssessmentLanguage, ImpactAssessment, ImpactAssessmentInput } from '@/api/types';
import { useAuth } from '@/auth/AuthProvider';
import { ADVISORY_LANGUAGES, languageOption, rememberPreferredLanguage } from '@/lib/languages';
import { cn } from '@/lib/cn';
import { formatInstant } from '@/lib/format';
import { publishScenario, useActiveScenario } from '@/store/scenarioStore';
import { Button, ErrorNote } from './ui';
import { ProvenanceChip } from './ProvenanceChip';

/**
 * The generated early-warning text, and what can be done with it.
 *
 * Three things live here because they are the same artefact at different moments: the text itself,
 * the CAP 1.2 document that other warning systems consume, and the push to a messaging channel. All
 * three carry provenance, because an operator deciding whether to act needs to know whether a
 * language model or the deterministic template wrote what they are reading, and whether a model was
 * attempted and failed.
 *
 * Re-generating in another language re-runs the assessment on the server rather than translating
 * locally: the advisory is a server artefact, and a client-side translation would be a second,
 * untracked source of truth for a warning.
 */
export function AdvisoryPanel({ assessment }: { assessment: ImpactAssessment }) {
  const scenario = useActiveScenario();
  const { canWriteAssets } = useAuth();
  const channel = useAdvisoryChannel();
  const assess = useAssessImpact();
  const dispatch = useDispatchAdvisory();

  const [copied, setCopied] = useState(false);
  const [capError, setCapError] = useState<unknown>(null);
  const [capBusy, setCapBusy] = useState(false);
  const [dispatchResult, setDispatchResult] = useState<AdvisoryDispatch | null>(null);
  const [dispatchError, setDispatchError] = useState<unknown>(null);

  const language = assessment.advisoryProvenance.language;

  /** The inputs that produced this assessment, when they are still on screen. */
  const inputs = useMemo<ImpactAssessmentInput | null>(() => {
    if (scenario === null || scenario.stormId !== assessment.stormId) {
      return null;
    }
    return {
      track: { stormId: scenario.stormId, points: scenario.points },
      assets: scenario.assets,
      evaluationTime: assessment.evaluatedAt,
    };
  }, [assessment.evaluatedAt, assessment.stormId, scenario]);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(assessment.advisory);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const changeLanguage = async (next: AssessmentLanguage) => {
    if (next === language) {
      return;
    }
    rememberPreferredLanguage(next);
    if (inputs === null || scenario === null) {
      return;
    }
    try {
      const regenerated = await assess.mutateAsync({ ...inputs, language: next });
      publishScenario({ ...scenario, assessment: regenerated });
    } catch {
      // Surfaced through assess.error below; the previous advisory stays on screen untouched.
    }
  };

  const downloadCap = async () => {
    if (inputs === null) {
      setCapError(new Error('The inputs behind this assessment are no longer on screen, so the alert cannot be rebuilt.'));
      return;
    }
    setCapBusy(true);
    setCapError(null);
    try {
      const { blob, filename } = await downloadCapAlert(inputs, language);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setCapError(cause);
    } finally {
      setCapBusy(false);
    }
  };

  const publish = async () => {
    if (inputs === null) {
      setDispatchError(new Error('The inputs behind this assessment are no longer on screen, so nothing can be published.'));
      return;
    }
    setDispatchError(null);
    setDispatchResult(null);
    try {
      setDispatchResult(await dispatch.mutateAsync({ input: inputs, language }));
    } catch (cause) {
      setDispatchError(cause);
    }
  };

  const channelConfigured = channel.data?.configured ?? false;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs text-ink-500">
          Storm <span className="font-mono font-semibold text-ink-700">{assessment.stormId}</span> · valid at{' '}
          {formatInstant(assessment.evaluatedAt)}
        </div>
        <ProvenanceChip assessment={assessment} />
      </div>

      <div className="flex flex-wrap items-end gap-4 rounded-xl border border-ink-200 bg-ink-50/60 px-4 py-3.5">
        <label className="block">
          <span className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
            <Languages className="size-3.5" />
            Advisory language
          </span>
          <select
            value={language}
            disabled={assess.isPending}
            onChange={(event) => void changeLanguage(event.target.value as AssessmentLanguage)}
            className="input mt-1.5 w-full min-w-[12rem] bg-white sm:w-auto"
          >
            {ADVISORY_LANGUAGES.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <p className="max-w-sm text-xs leading-relaxed text-ink-500">
          {languageOption(language).note}
          {assess.isPending ? ' Regenerating with the model…' : ''}
        </p>

        <div className="ml-auto flex flex-wrap items-center gap-2">
          <Button variant="secondary" onClick={copy}>
            {copied ? <Check className="size-4 text-accent-600" /> : <Copy className="size-4" />}
            {copied ? 'Copied' : 'Copy'}
          </Button>
          <Button variant="secondary" onClick={downloadCap} busy={capBusy}>
            {capBusy ? <Loader2 className="size-4 animate-spin" /> : <Download className="size-4" />}
            CAP 1.2
          </Button>
          <Button
            onClick={publish}
            busy={dispatch.isPending}
            disabled={!channelConfigured || !canWriteAssets}
            title={
              channelConfigured
                ? `Publish to the configured ${channel.data?.channel ?? 'notification'} channel`
                : channel.data?.detail ?? 'Checking the notification channel'
            }
          >
            <Send className="size-4" />
            Publish
          </Button>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-xl border border-ink-200 bg-ink-50/60">
        <div className="flex items-center gap-2 border-b border-ink-200/80 bg-white px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
          <FileCode2 className="size-3.5 text-brand-600" />
          Generated advisory text
          <span className="ml-auto font-mono normal-case tracking-normal text-ink-400">
            {languageOption(language).label}
          </span>
        </div>
        <pre className="max-h-[30rem] overflow-auto whitespace-pre-wrap break-words px-4 py-4 font-mono text-xs leading-relaxed text-ink-800">
          {assessment.advisory}
        </pre>
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        <p className="rounded-xl border border-ink-200 bg-white px-4 py-3 text-xs leading-relaxed text-ink-500">
          <span className="font-semibold text-ink-700">CAP 1.2</span> is the same assessment rendered as the
          warning format national and state systems already exchange, with the storm centre published as the alert
          area. Rule citations and asset identifiers stay in English in every language; they are what an operator
          reads off the instruments and types into other systems.
        </p>

        <div className="rounded-xl border border-ink-200 bg-white px-4 py-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-ink-700">
            <Radio className={cn('size-3.5', channelConfigured ? 'text-accent-600' : 'text-ink-400')} />
            {channelConfigured ? `Channel ready: ${channel.data?.channel}` : 'No notification channel configured'}
          </div>
          <p className="mt-1.5 text-xs leading-relaxed text-ink-500">
            {channel.data?.detail ??
              'Checking whether this deployment can push advisories to a messaging gateway or control room.'}
          </p>
          {dispatchResult !== null ? (
            <p
              className={cn(
                'mt-2 flex items-start gap-2 rounded-lg px-3 py-2 text-xs',
                dispatchResult.delivered ? 'bg-accent-50 text-accent-800' : 'bg-amber-50 text-amber-800',
              )}
            >
              {dispatchResult.delivered ? (
                <BadgeCheck className="mt-0.5 size-3.5 shrink-0" />
              ) : (
                <TriangleAlert className="mt-0.5 size-3.5 shrink-0" />
              )}
              <span>
                {dispatchResult.detail} · alert id{' '}
                <span className="font-mono">{dispatchResult.capIdentifier}</span>
                {dispatchResult.latencyMillis > 0 ? ` · ${dispatchResult.latencyMillis} ms` : ''}
              </span>
            </p>
          ) : null}
        </div>
      </div>

      <ErrorNote error={capError} />
      <ErrorNote error={dispatchError ?? dispatch.error ?? assess.error} />
    </div>
  );
}
