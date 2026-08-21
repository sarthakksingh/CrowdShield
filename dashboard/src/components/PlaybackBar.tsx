import React from 'react';
import { PlaybackState } from '../types';
import { Play, Pause, RotateCcw, Clock } from 'lucide-react';

interface PlaybackBarProps {
  playback: PlaybackState | null;
  onLoadScenario: (id: string) => void;
  onPlay: (speed?: number) => void;
  onPause: () => void;
  onSeek: (offsetSec: number) => void;
  onReset: () => void;
}

const SCENARIOS = [
  { id: 's1_normal_flow', label: '1. Normal Flow (Baseline)' },
  { id: 's2_entry_bottleneck', label: '2. Entry Bottleneck & Choke' },
  { id: 's3_counterflow_panic', label: '3. Counterflow & Conflict' },
  { id: 's4_post_intervention_recovery', label: '4. Post-Intervention Recovery' },
];

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

export const PlaybackBar: React.FC<PlaybackBarProps> = ({
  playback,
  onLoadScenario,
  onPlay,
  onPause,
  onSeek,
  onReset,
}) => {
  const currentOffset = playback?.currentOffsetSec ?? 0;
  const duration = playback?.durationSec ?? 240;
  const isPlaying = playback?.playing ?? false;
  const speed = playback?.playbackSpeed ?? 1.0;
  const currentScenario = playback?.scenarioId ?? 's1_normal_flow';

  return (
    <div
      style={{
        background: '#0f172a',
        borderBottom: '1px solid var(--border-color)',
        padding: '10px 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '14px',
      }}
    >
      {/* Scenario Dropdown */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <label style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>SCENARIO:</label>
        <select
          value={currentScenario}
          onChange={(e) => onLoadScenario(e.target.value)}
          style={{
            background: 'var(--bg-subtle)',
            border: '1px solid var(--border-color)',
            color: 'var(--text-primary)',
            padding: '6px 12px',
            borderRadius: '6px',
            fontSize: '0.85rem',
            fontWeight: 600,
            cursor: 'pointer',
            outline: 'none',
          }}
        >
          {SCENARIOS.map((sc) => (
            <option key={sc.id} value={sc.id}>
              {sc.label}
            </option>
          ))}
        </select>
      </div>

      {/* Media Controls & Timeline */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          flex: '1',
          maxWidth: '650px',
          margin: '0 12px',
        }}
      >
        {/* Play/Pause */}
        {isPlaying ? (
          <button className="btn btn-secondary" onClick={onPause} title="Pause">
            <Pause size={16} />
            <span>Pause</span>
          </button>
        ) : (
          <button className="btn btn-primary" onClick={() => onPlay(speed)} title="Play">
            <Play size={16} />
            <span>Play</span>
          </button>
        )}

        {/* Reset */}
        <button className="btn btn-secondary" onClick={onReset} title="Reset to t=0">
          <RotateCcw size={15} />
        </button>

        {/* Timeline Slider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: '1' }}>
          <input
            type="range"
            min="0"
            max={duration}
            step="10"
            value={currentOffset}
            onChange={(e) => onSeek(parseInt(e.target.value, 10))}
            style={{
              flex: '1',
              accentColor: '#38bdf8',
              cursor: 'pointer',
            }}
          />
          <div
            className="mono"
            style={{
              fontSize: '0.8rem',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              minWidth: '100px',
            }}
          >
            <Clock size={13} color="var(--text-muted)" />
            <span>
              {formatTime(currentOffset)} / {formatTime(duration)}
            </span>
          </div>
        </div>
      </div>

      {/* Speed Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>SPEED:</span>
        {[1.0, 2.0, 4.0].map((s) => (
          <button
            key={s}
            onClick={() => onPlay(s)}
            className="btn btn-secondary"
            style={{
              padding: '4px 8px',
              fontSize: '0.75rem',
              background: speed === s && isPlaying ? 'rgba(56, 189, 248, 0.2)' : undefined,
              borderColor: speed === s && isPlaying ? '#38bdf8' : undefined,
              color: speed === s && isPlaying ? '#38bdf8' : undefined,
            }}
          >
            {s}x
          </button>
        ))}
      </div>
    </div>
  );
};
