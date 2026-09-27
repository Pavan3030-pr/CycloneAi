import { useMemo, useState } from 'react';
import { useAssessGeoJson, useAssessImpact, useAssets } from '@/api/hooks';
import type { AssetInput, ImpactAssessment, TrackPointInput } from '@/api/types';
import { ExposureTable } from '@/components/ExposureTable';
import { MapLegend, TrackMap } from '@/components/TrackMap';
import { SummaryCards } from '@/components/SummaryCards';
import { Button, Card, ErrorNote, Field, Input, Select } from '@/components/ui';
import { bayOfBengalScenario } from '@/demo/demoScenario';
import { ASSET_TYPE_LABELS } from '@/lib/risk';
import { evaluationOptions, parseTrackPoints, trackPointsFromFeatureCollection } from '@/lib/track';
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
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant={mode === 'structured' ? 'primary' : 'secondary'} onClick={() => setMode('structured')}>
          Structured track
        </Button>
        <Button variant={mode === 'geojson' ? 'primary' : 'secondary'} onClick={() => setMode('geojson')}>
          Paste GeoJSON
        </Button>
      </div>

      {mode === 'structured' ? <StructuredAssessment /> : <GeoJsonAssessment />}
    </div>
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

  const availableAssets = useMemo(() => dedupeById([...registryAssets, ...scenario.assets]), [registryAssets, scenario.assets]);

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

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card title="Storm track" subtitle="Fixes may be listed in any order; the aggregate sorts them" className="xl:col-span-2">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <Field label="Storm identifier">
              <Input value={stormId} maxLength={32} onChange={(event) => setStormId(event.target.value)} />
            </Field>
            <Field label="Evaluation instant" hint={points === null ? 'Fix the JSON to choose a time' : 'Fixes and mid-interval points'}>
              <Select value={evaluationTime} onChange={(event) => setEvaluationTime(event.target.value)} disabled={options.length === 0}>
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

          <Field label="Track points (JSON)">
            <textarea
              value={pointsJson}
              onChange={(event) => setPointsJson(event.target.value)}
              spellCheck={false}
              rows={12}
              className="input font-mono text-xs"
            />
          </Field>

          <div className="mt-3 flex flex-wrap gap-2">
            <Button variant="secondary" onClick={() => setPointsJson(JSON.stringify(scenario.points, null, 2))}>
              Load demonstration track
            </Button>
            {assessmentSpans(points) ? <span className="self-center text-xs text-slate-500 dark:text-slate-400">{assessmentSpans(points)}</span> : null}
          </div>
        </Card>

        <Card
          title="Assets to assess"
          subtitle={`${selectedAssetIds.size} selected`}
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
          {registry.isPending ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">Loading the registry…</p>
          ) : null}
          <ul className="space-y-2">
            {availableAssets.map((asset) => (
              <li key={asset.id} className="flex items-start gap-3">
                <input
                  type="checkbox"
                  className="mt-1 size-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500 dark:border-slate-600"
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
                  <span className="block truncate text-sm text-slate-800 dark:text-slate-100">{asset.name}</span>
                  <span className="block text-[11px] text-slate-500 dark:text-slate-400">
                    {ASSET_TYPE_LABELS[asset.assetType]} · {asset.id}
                  </span>
                </span>
              </li>
            ))}
          </ul>
          {availableAssets.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">The registry is empty; the demonstration assets are shown instead.</p>
          ) : null}
        </Card>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={submit} busy={assess.isPending}>
          Run assessment
        </Button>
        <span className="text-xs text-slate-500 dark:text-slate-400">
          Sends the track and the selected assets to POST /api/v1/impact-assessments.
        </span>
      </div>

      <ErrorNote error={validationError ?? assess.error} />
      {result !== null ? <AssessmentResult assessment={result} points={points ?? []} assets={availableAssets.filter((a) => selectedAssetIds.has(a.id))} /> : null}
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
          <textarea
            value={documentText}
            onChange={(event) => setDocumentText(event.target.value)}
            spellCheck={false}
            rows={12}
            placeholder='{ "type": "FeatureCollection", "features": [ … ] }'
            className="input font-mono text-xs"
          />
        </Field>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <Button variant="secondary" onClick={() => setDocumentText(JSON.stringify(demoFeatureCollection(), null, 2))}>
            Load demonstration GeoJSON
          </Button>
          <Field label="Evaluation instant (optional)">
            <Input
              value={evaluationTime}
              onChange={(event) => setEvaluationTime(event.target.value)}
              placeholder="2023-12-04T12:00:00Z"
              className="w-64 font-mono text-xs"
            />
          </Field>
        </div>

        <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
          The server accepts NHC/JTWC property names (maxwind, mslp, validtime), skips non-Point features such as the drawn
          track line and the wind cone, and understands ATCF compact times.
        </p>
      </Card>

      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={submit} busy={assessGeoJson.isPending} disabled={documentText.trim().length === 0}>
          Run assessment from GeoJSON
        </Button>
        <span className="text-xs text-slate-500 dark:text-slate-400">
          Sends the document to POST /api/v1/impact-assessments/from-geojson
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
      <Card title={`Result for ${assessment.stormId}`} subtitle="Published to the dashboard, map and advisory panel">
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
        <div className="mt-3">
          <MapLegend />
        </div>
      </Card>
      <Card title="Exposures">
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
