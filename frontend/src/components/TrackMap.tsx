import { latLngBounds } from 'leaflet';
import { useEffect, useMemo } from 'react';
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';
import type { AssetExposure, AssetType, StormPosition, TrackPointInput } from '@/api/types';
import { cn } from '@/lib/cn';
import { formatDecimal, formatInstant } from '@/lib/format';
import { ASSET_TYPE_LABELS, RISK_STYLES } from '@/lib/risk';

/**
 * The situational picture: the forecast track, the interpolated storm centre and every asset.
 *
 * Markers are circles rather than icons on purpose. The default Leaflet marker needs image assets
 * that break under a bundler unless patched, and a coloured circle carries the risk level without a
 * legend lookup, which is the whole point of the view.
 */

export interface MapAsset {
  id: string;
  name: string;
  assetType: AssetType;
  latitude: number;
  longitude: number;
}

interface TrackMapProps {
  points: TrackPointInput[];
  stormPosition?: StormPosition | null;
  exposures?: AssetExposure[];
  assets?: MapAsset[];
  heightClass?: string;
}

export function TrackMap({
  points,
  stormPosition = null,
  exposures = [],
  assets = [],
  heightClass = 'h-[26rem]',
}: TrackMapProps) {
  const orderedTrack = useMemo(
    () => [...points].sort((left, right) => left.timestamp.localeCompare(right.timestamp)),
    [points],
  );
  const riskByAssetId = useMemo(
    () => new Map(exposures.map((exposure) => [exposure.assetId, exposure])),
    [exposures],
  );

  if (orderedTrack.length === 0) {
    return (
      <div className={cn('grid place-items-center rounded-lg border border-dashed border-slate-300 text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400', heightClass)}>
        No track to display yet.
      </div>
    );
  }

  const centre: [number, number] = [orderedTrack[0].latitude, orderedTrack[0].longitude];

  return (
    <div className={cn('overflow-hidden rounded-lg border border-slate-200 dark:border-slate-800', heightClass)}>
      <MapContainer center={centre} zoom={6} scrollWheelZoom className="size-full" worldCopyJump>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <Polyline
          positions={orderedTrack.map((point) => [point.latitude, point.longitude] as [number, number])}
          pathOptions={{ color: '#38bdf8', weight: 3, opacity: 0.9, dashArray: '6 6' }}
        />

        {orderedTrack.map((point) => (
          <CircleMarker
            key={point.timestamp}
            center={[point.latitude, point.longitude]}
            radius={4}
            pathOptions={{ color: '#0ea5e9', fillColor: '#0ea5e9', fillOpacity: 0.85, weight: 1 }}
          >
            <Popup>
              <div className="space-y-0.5 text-xs">
                <p className="font-semibold">Published fix</p>
                <p>{formatInstant(point.timestamp)}</p>
                <p>
                  {formatDecimal(point.latitude, 2)}°, {formatDecimal(point.longitude, 2)}°
                </p>
                <p>
                  {point.windSpeedKnots} kt · {point.centralPressureMb} mb
                </p>
              </div>
            </Popup>
          </CircleMarker>
        ))}

        {stormPosition !== null ? (
          <CircleMarker
            center={[stormPosition.latitude, stormPosition.longitude]}
            radius={11}
            pathOptions={{ color: '#f43f5e', fillColor: '#fb7185', fillOpacity: 0.55, weight: 2 }}
          >
            <Popup>
              <div className="space-y-0.5 text-xs">
                <p className="font-semibold">Interpolated storm centre</p>
                <p>
                  {stormPosition.category} · {stormPosition.windSpeedKnots} kt
                </p>
                <p>{stormPosition.centralPressureMb} mb</p>
                <p className="font-mono">{stormPosition.coordinateString}</p>
              </div>
            </Popup>
          </CircleMarker>
        ) : null}

        {assets.map((asset) => {
          const exposure = riskByAssetId.get(asset.id);
          const colour = exposure === undefined ? '#64748b' : RISK_STYLES[exposure.riskLevel].hex;
          return (
            <CircleMarker
              key={asset.id}
              center={[asset.latitude, asset.longitude]}
              radius={7}
              pathOptions={{ color: colour, fillColor: colour, fillOpacity: 0.8, weight: 2 }}
            >
              <Popup>
                <div className="space-y-0.5 text-xs">
                  <p className="font-semibold">{asset.name}</p>
                  <p>{ASSET_TYPE_LABELS[asset.assetType]}</p>
                  <p className="font-mono">{asset.id}</p>
                  {exposure === undefined ? (
                    <p>Not assessed</p>
                  ) : (
                    <>
                      <p className="font-semibold" style={{ color: RISK_STYLES[exposure.riskLevel].hex }}>
                        {RISK_STYLES[exposure.riskLevel].label} risk
                      </p>
                      <p>
                        {formatDecimal(exposure.distanceNauticalMiles)} nm · {exposure.estimatedWindAtAsset} kt
                      </p>
                      <p className="max-w-[15rem]">{exposure.rationale}</p>
                    </>
                  )}
                </div>
              </Popup>
            </CircleMarker>
          );
        })}

        <FitToContent points={orderedTrack} stormPosition={stormPosition} assets={assets} />
      </MapContainer>
    </div>
  );
}

/**
 * Keeps every drawn feature in view: Leaflet has no idea that our markers changed, so the bounds
 * have to be pushed back into it whenever the scenario changes.
 */
function FitToContent({
  points,
  stormPosition,
  assets,
}: {
  points: TrackPointInput[];
  stormPosition: StormPosition | null;
  assets: MapAsset[];
}) {
  const map = useMap();

  useEffect(() => {
    const positions: Array<[number, number]> = [
      ...points.map((point) => [point.latitude, point.longitude] as [number, number]),
      ...assets.map((asset) => [asset.latitude, asset.longitude] as [number, number]),
    ];
    if (stormPosition !== null) {
      positions.push([stormPosition.latitude, stormPosition.longitude]);
    }
    if (positions.length === 0) {
      return;
    }
    map.fitBounds(latLngBounds(positions), { padding: [32, 32] });
  }, [assets, map, points, stormPosition]);

  return null;
}

/** Legend for the map, exported so screens can place it beside or below the canvas. */
export function MapLegend() {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-600 dark:text-slate-300">
      <span className="flex items-center gap-1.5">
        <span className="inline-block h-0.5 w-6 border-t-2 border-dashed border-sky-400" />
        Forecast track
      </span>
      <span className="flex items-center gap-1.5">
        <span className="inline-block size-2.5 rounded-full bg-rose-400" />
        Interpolated centre
      </span>
      {(Object.keys(RISK_STYLES) as Array<keyof typeof RISK_STYLES>).map((level) => (
        <span key={level} className="flex items-center gap-1.5">
          <span className="inline-block size-2.5 rounded-full" style={{ background: RISK_STYLES[level].hex }} />
          {RISK_STYLES[level].label}
        </span>
      ))}
      <span className="flex items-center gap-1.5">
        <span className="inline-block size-2.5 rounded-full bg-slate-500" />
        Not assessed
      </span>
    </div>
  );
}
