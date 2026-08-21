import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  EventInfo,
  PlaybackState,
  RiskResponse,
  CurrentAnalyticsResponse,
  AlertRecord,
  RecommendationsResponse,
  Recommendation,
} from './types';
import { api } from './api/client';
import { useSSE } from './hooks/useSSE';
import { Header } from './components/Header';
import { PlaybackBar } from './components/PlaybackBar';
import { VenueOverview } from './components/VenueOverview';
import { RiskSummaryPanel } from './components/RiskSummaryPanel';
import { ZoneDetailPanel } from './components/ZoneDetailPanel';
import { RecommendationPanel } from './components/RecommendationPanel';
import { SimulationSandbox } from './components/SimulationSandbox';
import { AlertsFeed } from './components/AlertsFeed';

export const App: React.FC = () => {
  const [event, setEvent] = useState<EventInfo | null>(null);
  const [playback, setPlayback] = useState<PlaybackState | null>(null);
  const [risk, setRisk] = useState<RiskResponse | null>(null);
  const [analytics, setAnalytics] = useState<CurrentAnalyticsResponse | null>(null);
  const [alerts, setAlerts] = useState<AlertRecord[]>([]);
  const [recommendations, setRecommendations] = useState<RecommendationsResponse | null>(null);
  const [selectedZoneId, setSelectedZoneId] = useState<string>('z-corridor-1');
  const [stagedRecommendation, setStagedRecommendation] = useState<Recommendation | null>(null);

  const isFetchingRef = useRef(false);

  // Core refresh function
  const refreshData = useCallback(async () => {
    if (isFetchingRef.current) return;
    isFetchingRef.current = true;
    try {
      const [
        eventData,
        playbackData,
        riskData,
        analyticsData,
        alertsData,
        recommendationsData,
      ] = await Promise.all([
        api.getEvent().catch(() => null),
        api.getPlaybackState().catch(() => null),
        api.getCurrentRisk().catch(() => null),
        api.getCurrentAnalytics().catch(() => null),
        api.getAlerts().catch(() => []),
        api.getRecommendations().catch(() => null),
      ]);

      if (eventData) setEvent(eventData);
      if (playbackData) setPlayback(playbackData);
      if (riskData) setRisk(riskData);
      if (analyticsData) setAnalytics(analyticsData);
      if (alertsData) setAlerts(Array.isArray(alertsData) ? alertsData : []);
      if (recommendationsData) setRecommendations(recommendationsData);
    } catch (err) {
      console.error('Data refresh error:', err);
    } finally {
      isFetchingRef.current = false;
    }
  }, []);

  // Initial load
  useEffect(() => {
    refreshData();
  }, [refreshData]);

  // Connect SSE Live Stream
  useSSE(refreshData);

  // Periodic fallback polling when playing
  useEffect(() => {
    if (!playback?.playing) return;
    const interval = setInterval(() => {
      refreshData();
    }, 1500);
    return () => clearInterval(interval);
  }, [playback?.playing, refreshData]);

  // Handlers
  const handleLoadScenario = async (scenarioId: string) => {
    try {
      const state = await api.loadScenario(scenarioId);
      setPlayback(state);
      await refreshData();
    } catch (err) {
      console.error('Failed to load scenario:', err);
    }
  };

  const handlePlay = async (speed: number = 1.0) => {
    try {
      const state = await api.playPlayback(speed);
      setPlayback(state);
    } catch (err) {
      console.error('Failed to play:', err);
    }
  };

  const handlePause = async () => {
    try {
      const state = await api.pausePlayback();
      setPlayback(state);
    } catch (err) {
      console.error('Failed to pause:', err);
    }
  };

  const handleSeek = async (offsetSec: number) => {
    try {
      const state = await api.seekPlayback(offsetSec);
      setPlayback(state);
      await refreshData();
    } catch (err) {
      console.error('Failed to seek:', err);
    }
  };

  const handleReset = async () => {
    try {
      const state = await api.resetPlayback();
      setPlayback(state);
      await refreshData();
    } catch (err) {
      console.error('Failed to reset:', err);
    }
  };

  return (
    <div className="app-container">
      {/* 1. Header */}
      <Header
        event={event}
        playback={playback}
        disclaimer={risk?.disclaimer || risk?.overallRisk.disclaimer}
      />

      {/* 2. Playback Bar */}
      <PlaybackBar
        playback={playback}
        onLoadScenario={handleLoadScenario}
        onPlay={handlePlay}
        onPause={handlePause}
        onSeek={handleSeek}
        onReset={handleReset}
      />

      {/* 3. Main Dashboard Grid */}
      <main className="dashboard-grid">
        {/* Left Column: Venue Overview, Risk Summary, Zone Detail */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <VenueOverview
            analytics={analytics}
            risk={risk}
            selectedZoneId={selectedZoneId}
            onSelectZone={setSelectedZoneId}
          />

          <RiskSummaryPanel risk={risk} />

          <ZoneDetailPanel zoneId={selectedZoneId} refreshTrigger={playback?.currentOffsetSec} />
        </div>

        {/* Right Column: Recommendations, Simulation Sandbox, Alerts */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <RecommendationPanel
            recommendationsResponse={recommendations}
            onSelectForSimulation={(rec) => setStagedRecommendation(rec)}
          />

          <SimulationSandbox
            scenarioId={playback?.scenarioId || 's1_normal_flow'}
            eventId={event?.eventId || 'event-tech-nova-2026'}
            stagedRecommendation={stagedRecommendation}
            onClearStaged={() => setStagedRecommendation(null)}
          />

          <AlertsFeed alerts={alerts} />
        </div>
      </main>
    </div>
  );
};

export default App;
