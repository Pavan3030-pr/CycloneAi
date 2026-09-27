import { useState } from 'react';
import type { ImpactAssessment } from '@/api/types';
import { formatInstant } from '@/lib/format';
import { Button } from './ui';

/**
 * The generated early-warning text, presented as the artefact it is.
 *
 * Advisories are copied out of this panel and pasted into briefings and chat channels, so the copy
 * affordance is the primary action and the text keeps its own line breaks rather than being
 * re-flowed by the surrounding layout.
 */
export function AdvisoryPanel({ assessment }: { assessment: ImpactAssessment }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(assessment.advisory);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs text-slate-500 dark:text-slate-400">
          Storm <span className="font-mono text-slate-700 dark:text-slate-200">{assessment.stormId}</span> · valid at{' '}
          {formatInstant(assessment.evaluatedAt)}
        </div>
        <Button variant="secondary" onClick={copy}>
          {copied ? 'Copied' : 'Copy advisory'}
        </Button>
      </div>
      <pre className="max-h-[28rem] overflow-auto whitespace-pre-wrap break-words rounded-lg border border-slate-200 bg-slate-50 p-4 font-mono text-xs leading-relaxed text-slate-800 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-200">
        {assessment.advisory}
      </pre>
    </div>
  );
}
