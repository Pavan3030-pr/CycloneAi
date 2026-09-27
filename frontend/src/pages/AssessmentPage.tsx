import { FileJson, ListOrdered, Play, ShieldAlert, Sparkles } from 'lucide-react';
import { useMemo, useState, type ReactNode } from 'react';
import { useAssessGeoJson, useAssessImpact, useAssets } from '@/api/hooks';
import type { AssetInput, ImpactAssessment, TrackPointInput } from '@/api/types';
import { ExposureTable } from '@/components/ExposureTable';
import { MapLegend, TrackMap } from '@/components/TrackMap';
import { SummaryCards } from '@/components/SummaryCards';
import { Button, Card, ErrorNote, Field, Input, PageHeader, Select, Textarea } from '@/components/ui';
import { bayOfBengalScenario } from '@/demo/demoScenario';
import { ASSET_TYPE_LABELS } from '@/lib/risk';
import { evaluationOptions, parseTrackPoints, trackPointsFromFeatureCollection } from '@/lib/track';
import { cn } from '@/lib/cn';
import { publishScenario } from '@/store/scenarioStore';

/**
 * Runs an assessment against the live API, either from an editable track or from a pasted GeoJSON
 * document.
 *
 * Both modes are the same use case on the server; the second exists because that is how real track
 * data arrives from an agency, and going through a text box should not require reshaping it by hand.
 * The evaluation instant is chosen from the track itself, including the midpoints between fixes, so
 * the interpolated path is easy to exercise without knowing the arithmetic.
 */
export function AssessmentPage() {
  const [mode, setMode] = useState<'structured' | 'geojson'>('structured');

  return (
    <>
      <PageHeader
        eyebrow="New run"
        title="Impact assessment"
        description="Submit a track and a set of assets to the exposure model. The result is published to the dashboard, the map and the advisory panel."
        actions={
          <div className="flex rounded-xl border border-ink-200 bg-white p-1 shadow-hair">
            <ModeButton active={mode === 'structured'} onClick={() => setMode('structured')} icon={<ListOrdered className="size-4" />}>
              Structured track
            </ModeButton>
            <ModeButton active={mode === 'geojson'} onClick={() => setMode('geojson')} icon={<FileJson className="size-4" />}>
              Paste GeoJSON
            </ModeButton>
          </div>
        }
      />

      {mode === 'structured' ? <StructuredAssessment /> : <GeoJsonAssessment />}
    </>
  );
}

function ModeButton({
  active,
  onClick,
  icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-semibold transition',
        active ? 'bg-brand-700 text-white shadow-glow' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
      )}
    >
      {icon}
      {children}
    </button>
  );
}

function StructuredAssessment() {
  const scenario = bayOfBengalScenario;
  const [stormId, setStormId] = useState(scenario.stormId);
  const [pointsJson, setPointsJson] = useState(() => JSON.stringify(scenario.points, null, 2));
  const [evaluationTime, setEvaluationTime] = useState(scenario.evaluationTime);
  const [selectedAssetIds, setSelectedAssetIds] = useState<Set<string>>(
    () => new Set(scenario.assets.map((asset) => asset.id)),
  );
  const [validationError, setValidationError] = useState<unknown>(null);
  const [result, setResult] = useState<ImpactAssessment | null>(null);

  const registry = useAssets();
  const assess = useAssessImpact();

  const registryAssets: AssetInput[] = useMemo(
    () =>
      (registry.data ?? []).map((asset) => ({
        id: asset.id,
        name: asset.name,
        assetType: asset.assetType,
        latitude: asset.latitude,
        longitude: asset.longitude,
      })),
    [registry.data],
  );

  const availableAssets = useMemo(
    () => dedupeById([...registryAssets, ...scenario.assets]),
    [registryAssets, scenario.assets],
  );

  const points = useMemo<TrackPointInput[] | null>(() => {
    try {
      return parseTrackPoints(pointsJson);
    } catch {
      return null;
    }
  }, [pointsJson]);

  const options = useMemo(() => (points === null ? [] : evaluationOptions(points)), [points]);

  const submit = async () => {
    setValidationError(null);
    let parsedPoints: TrackPointInput[];
    try {
      parsedPoints = parseTrackPoints(pointsJson);
    } catch (cause) {
      setValidationError(cause);
      return;
    }

    const assets = availableAssets.filter((asset) => selectedAssetIds.has(asset.id));
    if (assets.length === 0) {
      setValidationError(new Error('Select at least one asset to assess.'));
      return;
    }

    try {
      const assessment = await assess.mutateAsync({
        track: { stormId, points: parsedPoints },
        assets,
        evaluationTime: evaluationTime.length > 0 ? evaluationTime : null,
      });
      publishScenario({ stormId, points: parsedPoints, assets, assessment });
      setResult(assessment);
    } catch {
      // The mutation error is rendered below from assess.error.
    }
  };

  const selectedAssets = availableAssets.filter((asset) => selectedAssetIds.has(asset.id));

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card
          title="Storm track"
          subtitle="Fixes may be listed in any order; the track aggregate sorts and validates them"
          className="xl:col-span-2"
        >
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Storm identifier">
              <Input value={stormId} maxLength={32} onChange={(event) => setStormId(event.target.value)} />
            </Field>
            <Field
              label="Evaluation instant"
              hint={points === null ? 'Fix the JSON to choose a time' : 'Fixes and mid-interval points'}
            >
              <Select
                value={evaluationTime}
                onChange={(event) => setEvaluationTime(event.target.value)}
                disabled={options.length === 0}
              >
                {options.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Custom instant" hint="RFC 3339, optional">
              <Input
                value={evaluationTime}
                onChange={(event) => setEvaluationTime(event.target.value)}
                placeholder="2026-09-27T06:00:00Z"
              />
            </Field>
          </div>

          <div className="mt-4">
            <Field label="Track points (JSON)">
              <Textarea
                value={pointsJson}
                onChange={(event) => setPointsJson(event.target.value)}
                spellCheck={false}
                rows={12}
              />
            </Field>
          </div>

          <div className="mt-3 flex flex-wrap items-center gap-3">
            <Button variant="secondary" onClick={() => setPointsJson(JSON.stringify(scenario.points, null, 2))}>
              <Sparkles className="size-4" />
              Load demonstration track
            </Button>
            {assessmentSpans(points) !== null ? (
              <span className="font-mono text-xs text-ink-500">{assessmentSpans(points)}</span>
            ) : null}
          </div>
        </Card>

        <Card
          title="Assets to assess"
          subtitle={`${selectedAssetIds.size} of ${availableAssets.length} selected`}
          actions={
            <Button
              variant="ghost"
              onClick={() =>
                setSelectedAssetIds((current) =>
                  current.size === availableAssets.length ? new Set() : new Set(availableAssets.map((a) => a.id)),
                )
              }
            >
              {selectedAssetIds.size === availableAssets.length ? 'Clear' : 'Select all'}
            </Button>
          }
        >
          {registry.isPending ? <p className="text-sm text-ink-500">Loading the registry…</p> : null}
          <ul className="space-y-2.5">
            {availableAssets.map((asset) => (
              <li key={asset.id}>
                <label className="flex cursor-pointer items-start gap-3 rounded-lg p-2 transition hover:bg-ink-50">
                  <input
                    type="checkbox"
                    className="mt-0.5 size-4 rounded border-ink-300 text-brand-700 focus:ring-brand-500"
                    checked={selectedAssetIds.has(asset.id)}
                    onChange={(event) =>
                      setSelectedAssetIds((current) => {
                        const next = new Set(current);
                        if (event.target.checked) {
                          next.add(asset.id);
                        } else {
                          next.delete(asset.id);
                        }
                        return next;
                      })
                    }
                  />
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-ink-800">{asset.name}</span>
                    <span className="block text-[11px] text-ink-500">
                      {ASSET_TYPE_LABELS[asset.assetType]} · {asset.id}
                    </span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
          {availableAssets.length === 0 ? (
            <p className="text-sm text-ink-500">
              The registry is empty; the demonstration assets are offered instead.
            </p>
          ) : null}
        </Card>
      </div>

      <div className="card flex flex-wrap items-center gap-4 px-5 py-4">
        <Button onClick={submit} busy={assess.isPending} className="px-5">
          {assess.isPending ? 'Running' : 'Run assessment'}
          {assess.isPending ? null : <Play className="size-4" />}
        </Button>
        <span className="text-xs text-ink-500">
          Sends the track and {selectedAssets.length} selected {selectedAssets.length === 1 ? 'asset' : 'assets'} to{' '}
          <span className="font-mono">POST /api/v1/impact-assessments</span>
        </span>
      </div>

      <ErrorNote error={validationError ?? assess.error} />

      {result !== null ? (
        <AssessmentResult assessment={result} points={points ?? []} assets={selectedAssets} />
      ) : null}
    </div>
  );
}

function GeoJsonAssessment() {
  const scenario = bayOfBengalScenario;
  const [documentText, setDocumentText] = useState('');
  const [evaluationTime, setEvaluationTime] = useState('');
  const [validationError, setValidationError] = useState<unknown>(null);
  const [result, setResult] = useState<ImpactAssessment | null>(null);
  const [resultPoints, setResultPoints] = useState<TrackPointInput[]>([]);

  const assessGeoJson = useAssessGeoJson();
  const assets = scenario.assets;

  const submit = async () => {
    setValidationError(null);
    let parsedDocument: unknown;
    try {
      parsedDocument = JSON.parse(documentText);
    } catch {
      setValidationError(new Error('The document must be valid JSON.'));
      return;
    }

    try {
      const assessment = await assessGeoJson.mutateAsync({
        featureCollection: parsedDocument,
        assets,
        evaluationTime: evaluationTime.length > 0 ? evaluationTime : null,
      });
      const points = trackPointsFromFeatureCollection(parsedDocument);
      publishScenario({ stormId: assessment.stormId, points, assets, assessment });
      setResultPoints(points);
      setResult(assessment);
    } catch {
      // Rendered from assessGeoJson.error below.
    }
  };

  return (
    <div className="space-y-5">
      <Card title="GeoJSON track document" subtitle="A FeatureCollection exactly as an agency publishes it">
        <Field label="Feature collection">
          <Textarea
            value={documentText}
            onChange={(event) => setDocumentText(event.target.value)}
            spellCheck={false}
            rows={12}
            placeholder='{ "type": "FeatureCollection", "features": [ … ] }'
          />
        </Field>

        <div className="mt-4 flex flex-wrap items-end gap-4">
          <Button
            variant="secondary"
            onClick={() => setDocumentText(JSON.stringify(demoFeatureCollection(), null, 2))}
          >
            <Sparkles className="size-4" />
            Load demonstration GeoJSON
          </Button>
          <div className="w-full sm:w-72">
            <Field label="Evaluation instant (optional)">
              <Input
                value={evaluationTime}
                onChange={(event) => setEvaluationTime(event.target.value)}
                placeholder="2023-12-04T12:00:00Z"
                className="font-mono text-xs"
              />
            </Field>
          </div>
        </div>

        <p className="mt-4 rounded-xl border border-ink-200 bg-ink-50/60 px-4 py-3 text-xs leading-relaxed text-ink-600">
          The server accepts NHC / JTWC property names (<span className="font-mono">maxwind</span>,{' '}
          <span className="font-mono">mslp</span>, <span className="font-mono">validtime</span>), skips non-Point
          features such as a drawn track line or wind cone, and understands ATCF compact times like{' '}
          <span className="font-mono">2023120406</span>.
        </p>
      </Card>

      <div className="card flex flex-wrap items-center gap-4 px-5 py-4">
        <Button
          onClick={submit}
          busy={assessGeoJson.isPending}
          disabled={documentText.trim().length === 0}
          className="px-5"
        >
          {assessGeoJson.isPending ? 'Running' : 'Run assessment from GeoJSON'}
          {assessGeoJson.isPending ? null : <ShieldAlert className="size-4" />}
        </Button>
        <span className="text-xs text-ink-500">
          Sends the document to <span className="font-mono">POST /api/v1/impact-assessments/from-geojson</span>
        </span>
      </div>

      <ErrorNote error={validationError ?? assessGeoJson.error} />

      {result !== null ? <AssessmentResult assessment={result} points={resultPoints} assets={assets} /> : null}
    </div>
  );
}

function AssessmentResult({
  assessment,
  points,
  assets,
}: {
  assessment: ImpactAssessment;
  points: TrackPointInput[];
  assets: AssetInput[];
}) {
  return (
    <div className="space-y-5">
      <Card
        title={`Result for ${assessment.stormId}`}
        subtitle="Published to the dashboard, the map and the advisory panel"
      >
        <SummaryCards summary={assessment.summary} />
      </Card>
      <Card title="Spatial view" subtitle="Marker colour is the assessed risk level">
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
      <Card title="Exposures" subtitle="Most severe first, then nearest">
        <ExposureTable exposures={assessment.exposures} />
      </Card>
    </div>
  );
}

function assessmentSpans(points: TrackPointInput[] | null): string | null {
  if (points === null || points.length < 2) {
    return null;
  }
  const ordered = [...points].sort((left, right) => left.timestamp.localeCompare(right.timestamp));
  const hours = (Date.parse(ordered[ordered.length - 1].timestamp) - Date.parse(ordered[0].timestamp)) / 3_600_000;
  return `${points.length} points spanning ${hours.toFixed(1)} h`;
}

function dedupeById(assets: AssetInput[]): AssetInput[] {
  const byId = new Map<string, AssetInput>();
  for (const asset of assets) {
    if (!byId.has(asset.id)) {
      byId.set(asset.id, asset);
    }
  }
  return [...byId.values()];
}

/** The demonstration track expressed as the GeoJSON shape an agency actually publishes. */
function demoFeatureCollection(): unknown {
  return {
    type: 'FeatureCollection',
    features: bayOfBengalScenario.points.map((point) => ({
      type: 'Feature',
      properties: {
        stormid: bayOfBengalScenario.stormId,
        stormname: 'DEMO BAY OF BENGAL',
        basin: 'IO',
        validtime: point.timestamp,
        maxwind: point.windSpeedKnots,
        mslp: point.centralPressureMb,
        stormtype: 'TC',
      },
      geometry: { type: 'Point', coordinates: [point.longitude, point.latitude] },
    })),
  };
}
