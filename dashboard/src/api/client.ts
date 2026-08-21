import {
  CurrentAnalyticsResponse,
  EventInfo,
  PlaybackState,
  RecommendationsResponse,
  RiskResponse,
  SimulationRequest,
  SimulationResponse,
  ZoneAnalyticsDetailResponse,
  AlertRecord,
} from '../types';

const API_BASE = '';

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!res.ok) {
    let errorMsg = `HTTP ${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body.message) errorMsg = body.message;
      else if (body.error) errorMsg = body.error;
    } catch {
      // ignore
    }
    throw new Error(errorMsg);
  }

  return res.json();
}

export const api = {
  getEvent: () => fetchJson<EventInfo>('/api/events/current'),

  getPlaybackState: () => fetchJson<PlaybackState>('/api/playback/state'),
  loadScenario: (scenarioId: string) =>
    fetchJson<PlaybackState>('/api/playback/load', {
      method: 'POST',
      body: JSON.stringify({ scenarioId }),
    }),
  playPlayback: (speed: number = 1.0) =>
    fetchJson<PlaybackState>('/api/playback/play', {
      method: 'POST',
      body: JSON.stringify({ speed }),
    }),
  pausePlayback: () =>
    fetchJson<PlaybackState>('/api/playback/pause', {
      method: 'POST',
    }),
  seekPlayback: (offsetSec: number) =>
    fetchJson<PlaybackState>('/api/playback/seek', {
      method: 'POST',
      body: JSON.stringify({ offsetSec }),
    }),
  resetPlayback: () =>
    fetchJson<PlaybackState>('/api/playback/reset', {
      method: 'POST',
    }),

  getCurrentRisk: () => fetchJson<RiskResponse>('/api/risk/current'),

  getCurrentAnalytics: () =>
    fetchJson<CurrentAnalyticsResponse>('/api/analytics/current'),
  getZoneAnalytics: (zoneId: string) =>
    fetchJson<ZoneAnalyticsDetailResponse>(`/api/analytics/zones/${zoneId}`),

  getAlerts: () => fetchJson<AlertRecord[]>('/api/alerts'),

  getRecommendations: () =>
    fetchJson<RecommendationsResponse>('/api/recommendations/current'),

  runSimulation: (req: SimulationRequest) =>
    fetchJson<SimulationResponse>('/api/simulations', {
      method: 'POST',
      body: JSON.stringify(req),
    }),
};
