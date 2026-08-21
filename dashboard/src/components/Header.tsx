import React from 'react';
import { EventInfo, PlaybackState } from '../types';
import { Shield, Radio, AlertTriangle } from 'lucide-react';

interface HeaderProps {
  event: EventInfo | null;
  playback: PlaybackState | null;
  disclaimer?: string;
}

export const Header: React.FC<HeaderProps> = ({ event, playback, disclaimer }) => {
  const isPlaying = playback?.playing ?? false;

  return (
    <header
      style={{
        background: '#0d1322',
        borderBottom: '1px solid var(--border-color)',
        padding: '12px 24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        {/* Brand & Event */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div
            style={{
              background: 'linear-gradient(135deg, #1e3a8a, #0284c7)',
              borderRadius: '8px',
              padding: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 12px rgba(2, 132, 199, 0.4)',
            }}
          >
            <Shield size={24} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#ffffff' }}>
                CrowdShield
              </h1>
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: '4px',
                  background: 'rgba(56, 189, 248, 0.2)',
                  color: '#38bdf8',
                  border: '1px solid rgba(56, 189, 248, 0.4)',
                }}
              >
                OPS DASHBOARD
              </span>
            </div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              {event?.name || 'Main Ground Venue Monitoring'} • <span className="mono">{event?.venueId || 'venue-main-ground'}</span>
            </div>
          </div>
        </div>

        {/* Live Status & Scenario Pill */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              borderRadius: '6px',
              background: isPlaying ? 'rgba(16, 185, 129, 0.15)' : 'rgba(234, 179, 8, 0.15)',
              border: `1px solid ${isPlaying ? 'rgba(16, 185, 129, 0.4)' : 'rgba(234, 179, 8, 0.4)'}`,
              color: isPlaying ? '#10b981' : '#eab308',
              fontSize: '0.8rem',
              fontWeight: 600,
            }}
          >
            <Radio size={14} className={isPlaying ? 'animate-pulse' : ''} />
            <span>{isPlaying ? 'LIVE STREAMING' : 'PLAYBACK PAUSED'}</span>
          </div>

          <div
            style={{
              padding: '6px 12px',
              borderRadius: '6px',
              background: 'var(--bg-subtle)',
              border: '1px solid var(--border-color)',
              color: 'var(--text-primary)',
              fontSize: '0.8rem',
              fontWeight: 500,
            }}
          >
            <span style={{ color: 'var(--text-muted)', marginRight: '6px' }}>SCENARIO:</span>
            <strong className="mono">{playback?.scenarioName || playback?.scenarioId || 's1_normal_flow'}</strong>
          </div>
        </div>
      </div>

      {/* Safety Prototype Disclaimer Banner */}
      {disclaimer && (
        <div
          style={{
            background: 'rgba(234, 179, 8, 0.08)',
            border: '1px solid rgba(234, 179, 8, 0.25)',
            borderRadius: '6px',
            padding: '4px 10px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '0.75rem',
            color: '#fbbf24',
          }}
        >
          <AlertTriangle size={13} style={{ flexShrink: 0 }} />
          <span>{disclaimer}</span>
        </div>
      )}
    </header>
  );
};
