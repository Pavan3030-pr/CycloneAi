import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button, Card } from './ui';

/**
 * The first lines of the generated advisory.
 *
 * The opening lines carry the storm identity, validity time and intensity, which is what a duty
 * officer needs at a glance. The full text, including the model's stated limitations, stays one
 * click away rather than being truncated into this card.
 */
export function AdvisoryPreview({ advisory }: { advisory: string }) {
  const previewLines = advisory.split('\n').slice(0, 6);

  return (
    <Card
      title="Advisory"
      subtitle="First lines of the generated text"
      actions={
        <Link to="/app/advisory">
          <Button variant="ghost">
            Full text
            <ArrowRight className="size-3.5" />
          </Button>
        </Link>
      }
    >
      <pre className="overflow-hidden whitespace-pre-wrap break-words font-mono text-[11px] leading-relaxed text-ink-700">
        {previewLines.join('\n')}
      </pre>
    </Card>
  );
}
