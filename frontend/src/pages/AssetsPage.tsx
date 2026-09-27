import { useState, type FormEvent } from 'react';
import { useAssets, useDeleteAsset, useRegisterAsset } from '@/api/hooks';
import type { AssetType } from '@/api/types';
import { Button, Card, EmptyState, ErrorNote, Field, Input, Select, Spinner } from '@/components/ui';
import { useAuth } from '@/auth/AuthProvider';
import { bayOfBengalScenario } from '@/demo/demoScenario';
import { ASSET_TYPES, ASSET_TYPE_LABELS, ASSET_TYPE_SHORT } from '@/lib/risk';

/**
 * The tracked asset inventory, backed by the real registry endpoints.
 *
 * This is genuine server state, not a client-side list: an asset added here is available to every
 * subsequent assessment, and one deleted here disappears for everyone. Write controls are hidden for
 * a VIEWER, but the hiding is only cosmetic; the API rejects the write regardless, which is what
 * actually protects the data.
 */
export function AssetsPage() {
  const { canWriteAssets, principal } = useAuth();
  const registry = useAssets();
  const registerAsset = useRegisterAsset();
  const deleteAsset = useDeleteAsset();

  const [id, setId] = useState('');
  const [name, setName] = useState('');
  const [assetType, setAssetType] = useState<AssetType>('POWER_GRID');
  const [latitude, setLatitude] = useState('13.08');
  const [longitude, setLongitude] = useState('80.28');
  const [formError, setFormError] = useState<unknown>(null);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    try {
      await registerAsset.mutateAsync({
        id: id.trim(),
        name: name.trim(),
        assetType,
        latitude: Number(latitude),
        longitude: Number(longitude),
      });
      setId('');
      setName('');
    } catch (cause) {
      setFormError(cause);
    }
  };

  const loadDemoAssets = async () => {
    setFormError(null);
    try {
      await Promise.all(bayOfBengalScenario.assets.map((asset) => registerAsset.mutateAsync(asset)));
    } catch (cause) {
      setFormError(cause);
    }
  };

  const assets = registry.data ?? [];

  return (
    <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
      <Card
        title="Infrastructure registry"
        subtitle={`${assets.length} asset${assets.length === 1 ? '' : 's'} stored`}
        className="xl:col-span-2"
        actions={
          registry.isFetching ? <Spinner /> : (
            <Button variant="secondary" onClick={loadDemoAssets} disabled={!canWriteAssets}>
              Add demonstration assets
            </Button>
          )
        }
      >
        <ErrorNote error={registry.error} />

        {assets.length === 0 && !registry.isPending ? (
          <EmptyState
            title="No assets registered yet"
            description="Assets registered here are offered to every assessment. The demonstration set covers the Bay of Bengal coastline."
            action={
              <Button onClick={loadDemoAssets} disabled={!canWriteAssets}>
                Add demonstration assets
              </Button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[34rem] border-collapse text-sm">
              <thead>
                <tr className="text-left text-[11px] uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  <th className="pb-2 pr-3 font-medium">Asset</th>
                  <th className="pb-2 pr-3 font-medium">Type</th>
                  <th className="pb-2 pr-3 font-medium">Position</th>
                  <th className="pb-2 font-medium" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {assets.map((asset) => (
                  <tr key={asset.id}>
                    <td className="py-2.5 pr-3">
                      <p className="font-medium text-slate-800 dark:text-slate-100">{asset.name}</p>
                      <p className="font-mono text-[11px] text-slate-500 dark:text-slate-400">{asset.id}</p>
                    </td>
                    <td className="py-2.5 pr-3 text-slate-600 dark:text-slate-300">
                      <span className="badge bg-slate-500/10 text-slate-600 ring-slate-400/30 dark:text-slate-300">
                        {ASSET_TYPE_SHORT[asset.assetType]}
                      </span>
                    </td>
                    <td className="py-2.5 pr-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {asset.coordinateString}
                    </td>
                    <td className="py-2.5 text-right">
                      <Button
                        variant="ghost"
                        disabled={!canWriteAssets || deleteAsset.isPending}
                        onClick={() => deleteAsset.mutate(asset.id)}
                      >
                        Remove
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <ErrorNote error={deleteAsset.error} />
      </Card>

      <Card title="Register an asset" subtitle="Reusing an identifier replaces that asset">
        {canWriteAssets ? (
          <form className="space-y-3" onSubmit={onSubmit}>
            <Field label="Identifier" hint="Stable registry key, letters, digits, dot, underscore and hyphen">
              <Input value={id} onChange={(event) => setId(event.target.value)} maxLength={64} required placeholder="DEMO-GRID-01" />
            </Field>
            <Field label="Name">
              <Input value={name} onChange={(event) => setName(event.target.value)} maxLength={256} required placeholder="Coastal substation" />
            </Field>
            <Field label="Asset type">
              <Select value={assetType} onChange={(event) => setAssetType(event.target.value as AssetType)}>
                {ASSET_TYPES.map((candidate) => (
                  <option key={candidate} value={candidate}>
                    {ASSET_TYPE_LABELS[candidate]}
                  </option>
                ))}
              </Select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Latitude">
                <Input value={latitude} onChange={(event) => setLatitude(event.target.value)} inputMode="decimal" required />
              </Field>
              <Field label="Longitude">
                <Input value={longitude} onChange={(event) => setLongitude(event.target.value)} inputMode="decimal" required />
              </Field>
            </div>
            <Button type="submit" busy={registerAsset.isPending} className="w-full">
              Register asset
            </Button>
            {formError !== null ? <ErrorNote error={formError} /> : null}
          </form>
        ) : (
          <div className="space-y-2">
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Signed in as <span className="font-medium">{principal?.username}</span> with{' '}
              <span className="font-mono text-xs">[{principal?.roles.join(', ')}]</span>.
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Writing to the registry requires the ANALYST or ADMIN role. The API enforces this independently of the
              interface.
            </p>
          </div>
        )}
      </Card>
    </div>
  );
}
