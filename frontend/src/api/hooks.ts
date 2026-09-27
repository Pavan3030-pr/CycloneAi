import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest, fetchHealth } from './client';
import type {
  Asset,
  AssetInput,
  GeoJsonAssessmentInput,
  ImpactAssessment,
  ImpactAssessmentInput,
} from './types';

/**
 * Server state, owned by React Query.
 *
 * The registry is cached and invalidated on write, so the asset list a user edits is the same list
 * an assessment submits without a manual refresh. Assessments are mutations rather than queries
 * because they are computed from a payload the user supplies rather than identified by a key; the
 * calling screen publishes the result to the scenario store so the map and advisory can read it.
 */

export const queryKeys = {
  health: ['health'] as const,
  assets: ['assets'] as const,
};

export function useApiHealth() {
  return useQuery({
    queryKey: queryKeys.health,
    queryFn: fetchHealth,
    refetchInterval: 30_000,
    retry: false,
  });
}

export function useAssets(enabled = true) {
  return useQuery({
    queryKey: queryKeys.assets,
    queryFn: () => apiRequest<Asset[]>('/api/v1/assets'),
    enabled,
  });
}

export function useRegisterAsset() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: AssetInput) =>
      apiRequest<Asset>('/api/v1/assets', { method: 'POST', body: JSON.stringify(input) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.assets }),
  });
}

export function useDeleteAsset() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiRequest<void>(`/api/v1/assets/${encodeURIComponent(id)}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.assets }),
  });
}

export function useAssessImpact() {
  return useMutation({
    mutationFn: (input: ImpactAssessmentInput) =>
      apiRequest<ImpactAssessment>('/api/v1/impact-assessments', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
  });
}

export function useAssessGeoJson() {
  return useMutation({
    mutationFn: (input: GeoJsonAssessmentInput) =>
      apiRequest<ImpactAssessment>('/api/v1/impact-assessments/from-geojson', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
  });
}
