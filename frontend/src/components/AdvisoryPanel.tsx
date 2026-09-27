import { Check, Copy, Download, FileText } from 'lucide-react';
import { useState } from 'react';
import type { ImpactAssessment } from '@/api/types';
import { formatInstant } from '@/lib/format';
import { Button } from './ui';

/**
 * The generated early-warning text, presented as the artefact it is.
 *
 * Advisories get copied into briefings, printed and read aloud, so the copy affordance is the primary
 * action, a download is offered beside it, and the text keeps its own line breaks rather than being
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

  const download = () => {
    const blob = new Blob([assessment.advisory], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${assessment.stormId}-advisory-${assessment.evaluatedAt.replace(/[:]/g, '')}.txt`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs text-ink-500">
          Storm <span className="font-mono font-semibold text-ink-700">{assessment.stormId}</span> · valid at{' '}
          {formatInstant(assessment.evaluatedAt)}
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={download}>
            <Download className="size-4" />
            Download
          </Button>
          <Button variant="secondary" onClick={copy}>
            {copied ? <Check className="size-4 text-accent-600" /> : <Copy className="size-4" />}
            {copied ? 'Copied' : 'Copy advisory'}
          </Button>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-xl border border-ink-200 bg-ink-50/60">
        <div className="flex items-center gap-2 border-b border-ink-200/80 bg-white px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-500">
          <FileText className="size-3.5 text-brand-600" />
          Generated advisory text
        </div>
        <pre className="max-h-[28rem] overflow-auto whitespace-pre-wrap break-words px-4 py-4 font-mono text-xs leading-relaxed text-ink-800">
          {assessment.advisory}
        </pre>
      </div>
    </div>
  );
}
