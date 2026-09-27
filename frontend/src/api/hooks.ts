import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest, dispatchAdvisory, fetchAdvisoryChannel, fetchHealth } from './client';
import type {
  AssessmentLanguage,
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
  advisoryChannel: ['advisoryChannel'] as const,
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

/**
 * Publishes an advisory to the configured channel.
 *
 * A mutation, not a query: it has a side effect on the real world, it is not idempotent in the sense
 * that matters (two calls send two messages), and the result is worth showing exactly once.
 */
export function useDispatchAdvisory() {
  return useMutation({
    mutationFn: ({ input, language }: { input: ImpactAssessmentInput; language: AssessmentLanguage }) =>
      dispatchAdvisory(input, language),
    retry: 0,
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

/**
 * Whether an outbound notification channel is configured on this deployment.
 *
 * Fetched rather than assumed, so the console can offer publishing only when publishing is actually
 * possible, and can explain the absence in the operator's own words when it is not.
 */
export function useAdvisoryChannel() {
  return useQuery({
    queryKey: queryKeys.advisoryChannel,
    queryFn: fetchAdvisoryChannel,
    staleTime: 60_000,
    retry: false,
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
