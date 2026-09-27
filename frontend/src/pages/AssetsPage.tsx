import { Building2, Info, Plus, ServerCog, Trash2 } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { useAssets, useDeleteAsset, useRegisterAsset } from '@/api/hooks';
import type { AssetType } from '@/api/types';
import { Button, Card, EmptyState, ErrorNote, Field, Input, PageHeader, Select, Spinner } from '@/components/ui';
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
  const [pendingId, setPendingId] = useState<string | null>(null);

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
    <>
      <PageHeader
        eyebrow="Inventory"
        title="Asset registry"
        description="Every asset registered here is offered to the next assessment. Reusing an identifier replaces that asset rather than duplicating it."
        actions={
          <>
            {registry.isFetching ? <Spinner className="text-ink-400" /> : null}
            <Button variant="secondary" onClick={loadDemoAssets} disabled={!canWriteAssets || registerAsset.isPending}>
              <Plus className="size-4" />
              Add demonstration assets
            </Button>
          </>
        }
      />

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card
          title="Registered infrastructure"
          subtitle={`${assets.length} asset${assets.length === 1 ? '' : 's'} stored`}
          className="xl:col-span-2"
        >
          <ErrorNote error={registry.error} />

          {assets.length === 0 && !registry.isPending ? (
            <EmptyState
              icon={<Building2 className="size-5" />}
              title="No assets registered yet"
              description="The demonstration set covers the Bay of Bengal coastline: two grid nodes, two highway spans and three medical shelters."
              action={
                <Button onClick={loadDemoAssets} disabled={!canWriteAssets || registerAsset.isPending}>
                  Add demonstration assets
                </Button>
              }
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[34rem] border-collapse text-sm">
                <thead>
                  <tr className="text-left text-[11px] uppercase tracking-[0.12em] text-ink-500">
                    <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Asset</th>
                    <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Type</th>
                    <th className="border-b border-ink-200 pb-2.5 pr-3 font-semibold">Position</th>
                    <th className="border-b border-ink-200 pb-2.5 font-semibold" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {assets.map((asset) => (
                    <tr key={asset.id} className="transition hover:bg-ink-50/70">
                      <td className="py-3 pr-3">
                        <p className="font-semibold text-ink-900">{asset.name}</p>
                        <p className="font-mono text-[11px] text-ink-500">{asset.id}</p>
                      </td>
                      <td className="py-3 pr-3">
                        <span className="badge bg-ink-100 text-ink-600 ring-ink-200">
                          {ASSET_TYPE_SHORT[asset.assetType]}
                        </span>
                      </td>
                      <td className="py-3 pr-3 font-mono text-xs text-ink-600">{asset.coordinateString}</td>
                      <td className="py-3 text-right">
                        <Button
                          variant="ghost"
                          aria-label={`Remove ${asset.name}`}
                          disabled={!canWriteAssets || deleteAsset.isPending}
                          onClick={() => {
                            setPendingId(asset.id);
                            deleteAsset.mutate(asset.id, { onSettled: () => setPendingId(null) });
                          }}
                        >
                          {pendingId === asset.id && deleteAsset.isPending ? (
                            <Spinner />
                          ) : (
                            <Trash2 className="size-4 text-coral-600" />
                          )}
                          <span className="hidden sm:inline">Remove</span>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <ErrorNote error={deleteAsset.error} className="mt-4" />
        </Card>

        {canWriteAssets ? (
          <Card title="Register an asset" subtitle="Writes go straight to the live registry">
            <form className="space-y-4" onSubmit={onSubmit}>
              <Field label="Identifier" hint="Stable registry key: letters, digits, dot, underscore, hyphen">
                <Input
                  value={id}
                  onChange={(event) => setId(event.target.value)}
                  maxLength={64}
                  required
                  placeholder="DEMO-GRID-01"
                />
              </Field>
              <Field label="Name">
                <Input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  maxLength={256}
                  required
                  placeholder="Coastal substation"
                />
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
                  <Input
                    value={longitude}
                    onChange={(event) => setLongitude(event.target.value)}
                    inputMode="decimal"
                    required
                  />
                </Field>
              </div>
              <Button type="submit" busy={registerAsset.isPending} className="w-full">
                Register asset
              </Button>
              <ErrorNote error={formError} />
            </form>
          </Card>
        ) : (
          <Card title="Read-only access">
            <div className="flex gap-3 rounded-xl border border-brand-100 bg-brand-50/70 p-4">
              <Info className="mt-0.5 size-4 shrink-0 text-brand-700" />
              <div className="text-sm text-ink-700">
                <p>
                  Signed in as <span className="font-semibold">{principal?.username}</span> with{' '}
                  <span className="font-mono text-xs">[{principal?.roles.join(', ')}]</span>.
                </p>
                <p className="mt-2 text-xs leading-relaxed text-ink-600">
                  Writing to the registry requires the ANALYST or ADMIN role. Hiding these controls is cosmetic —
                  the API enforces the same rule independently, and returns 403 for a VIEWER regardless of what the
                  interface allows.
                </p>
              </div>
            </div>
            <div className="mt-5 flex items-start gap-3 text-xs leading-relaxed text-ink-500">
              <ServerCog className="mt-0.5 size-4 shrink-0 text-ink-400" />
              Assets are stored in the running API instance, keyed by identifier, and shared by every assessment.
            </div>
          </Card>
        )}
      </div>
    </>
  );
}
