import React, { useState } from 'react';
import { SimulationAction, SimulationResponse, Recommendation } from '../types';
import { api } from '../api/client';
import { Sliders, Play, Plus, Trash2, ArrowRight, AlertTriangle, RefreshCw } from 'lucide-react';

interface SimulationSandboxProps {
  scenarioId: string;
  eventId: string;
  stagedRecommendation: Recommendation | null;
  onClearStaged: () => void;
}

const ACTION_OPTIONS = [
  { value: 'RESTRICT_GATE', label: 'RESTRICT_GATE (Meter Entry Flow)' },
  { value: 'OPEN_ROUTE', label: 'OPEN_ROUTE (Open Bypass Corridor)' },
  { value: 'OPEN_EXIT', label: 'OPEN_EXIT (Open Auxiliary Egress)' },
  { value: 'REDIRECT_INFLOW', label: 'REDIRECT_INFLOW (Enforce 1-Way Flow)' },
  { value: 'DEPLOY_PERSONNEL', label: 'DEPLOY_PERSONNEL (Dispatch Stewards)' },
];

const ZONE_OPTIONS = [
  { value: 'z-entry-a', label: 'Entry Gate A (z-entry-a)' },
  { value: 'z-corridor-1', label: 'Corridor 1 (z-corridor-1)' },
  { value: 'z-open-yard', label: 'Open Yard (z-open-yard)' },
  { value: 'z-exit-east', label: 'Exit East (z-exit-east)' },
];

export const SimulationSandbox: React.FC<SimulationSandboxProps> = ({
  scenarioId,
  eventId,
  stagedRecommendation,
  onClearStaged,
}) => {
  const [actions, setActions] = useState<SimulationAction[]>([
    { type: 'RESTRICT_GATE', targetId: 'z-entry-a', atOffsetSec: 0 },
    { type: 'OPEN_ROUTE', targetId: 'z-corridor-1', atOffsetSec: 0 },
  ]);

  const [newType, setNewType] = useState<string>('RESTRICT_GATE');
  const [newTarget, setNewTarget] = useState<string>('z-corridor-1');
  const [simResult, setSimResult] = useState<SimulationResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // If a recommendation was clicked to stage
  React.useEffect(() => {
    if (stagedRecommendation) {
      setActions([
        {
          type: stagedRecommendation.actionType,
          targetId: stagedRecommendation.targetZoneId,
          atOffsetSec: 0,
        },
      ]);
      onClearStaged();
    }
  }, [stagedRecommendation, onClearStaged]);

  const addAction = () => {
    setActions([...actions, { type: newType, targetId: newTarget, atOffsetSec: 0 }]);
  };

  const removeAction = (index: number) => {
    setActions(actions.filter((_, i) => i !== index));
  };

  const handleRunSimulation = async () => {
    if (actions.length === 0) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api.runSimulation({
        eventId: eventId || 'event-tech-nova-2026',
        scenarioName: `sim-${scenarioId}`,
        actions,
        horizonSec: 240,
      });
      setSimResult(res);
    } catch (err: any) {
      setError(err.message || 'Simulation execution failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">
          <Sliders size={17} color="#38bdf8" />
          Intervention Impact Simulation Sandbox
        </span>
        <span className="badge badge-advisory">STATELESS PROJECTION</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {/* Action Builder Controls */}
        <div
          style={{
            background: 'var(--bg-subtle)',
            borderRadius: '6px',
            padding: '12px',
            display: 'flex',
            flexDirection: 'column',
            gap: '10px',
          }}
        >
          <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
            1. Configure Proposed Interventions (Target Gates / Routes / Zones)
          </div>

          {/* Staged Actions List */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {actions.map((act, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  background: 'rgba(0, 0, 0, 0.25)',
                  padding: '6px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className="mono" style={{ fontWeight: 700, color: '#38bdf8' }}>
                    {act.type}
                  </span>
                  <ArrowRight size={12} color="var(--text-muted)" />
                  <span className="mono" style={{ color: '#ffffff' }}>
                    {act.targetId}
                  </span>
                </div>
                <button
                  className="btn btn-secondary"
                  onClick={() => removeAction(idx)}
                  style={{ padding: '2px 6px', color: '#f87171' }}
                  title="Remove"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            ))}
          </div>

          {/* Add Action Bar */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
            <select
              value={newType}
              onChange={(e) => setNewType(e.target.value)}
              style={{
                background: 'var(--bg-card)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-primary)',
                padding: '6px 10px',
                borderRadius: '4px',
                fontSize: '0.8rem',
                flex: '1',
              }}
            >
              {ACTION_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>

            <select
              value={newTarget}
              onChange={(e) => setNewTarget(e.target.value)}
              style={{
                background: 'var(--bg-card)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-primary)',
                padding: '6px 10px',
                borderRadius: '4px',
                fontSize: '0.8rem',
                flex: '1',
              }}
            >
              {ZONE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>

            <button className="btn btn-secondary" onClick={addAction} style={{ padding: '6px 10px' }}>
              <Plus size={14} /> Add Action
            </button>
          </div>

          {/* Run Button */}
          <button
            className="btn btn-primary"
            onClick={handleRunSimulation}
            disabled={loading || actions.length === 0}
            style={{ marginTop: '4px', padding: '8px 14px', width: '100%' }}
          >
            {loading ? (
              <>
                <RefreshCw size={15} className="animate-spin" />
                <span>Simulating Venue Flow Dynamics...</span>
              </>
            ) : (
              <>
                <Play size={15} />
                <span>Run Before/After Simulation Analysis</span>
              </>
            )}
          </button>
        </div>

        {error && (
          <div style={{ color: '#ef4444', fontSize: '0.8rem', padding: '6px 10px', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px' }}>
            {error}
          </div>
        )}

        {/* Simulation Output Section */}
        {simResult && (
          <div
            style={{
              background: '#0d1527',
              border: '1px solid var(--border-color)',
              borderRadius: '6px',
              padding: '14px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
            }}
          >
            {/* Verdict Row */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>PROJECTED VERDICT:</span>
                <span
                  className={`badge ${
                    simResult.verdict === 'IMPROVED'
                      ? 'badge-low'
                      : simResult.verdict === 'WORSENED'
                      ? 'badge-critical'
                      : 'badge-moderate'
                  }`}
                  style={{ fontSize: '0.85rem' }}
                >
                  {simResult.verdict}
                </span>
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                Scenario: <strong className="mono">{simResult.scenarioName}</strong>
              </span>
            </div>

            {/* Before vs After Metric Cards */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr 1fr',
                gap: '8px',
                textAlign: 'center',
              }}
            >
              <div style={{ background: 'var(--bg-subtle)', padding: '8px', borderRadius: '4px' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>BASELINE PEAK RISK</div>
                <div className="mono" style={{ fontSize: '1.2rem', fontWeight: 800, color: '#f87171' }}>
                  {simResult.baseline.peakRisk.toFixed(2)}
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>{simResult.baseline.level}</div>
              </div>

              <div style={{ background: 'var(--bg-subtle)', padding: '8px', borderRadius: '4px' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>PROJECTED PEAK RISK</div>
                <div className="mono" style={{ fontSize: '1.2rem', fontWeight: 800, color: '#34d399' }}>
                  {simResult.projected.peakRisk.toFixed(2)}
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>{simResult.projected.level}</div>
              </div>

              <div style={{ background: 'var(--bg-subtle)', padding: '8px', borderRadius: '4px' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>RISK DELTA</div>
                <div
                  className="mono"
                  style={{
                    fontSize: '1.2rem',
                    fontWeight: 800,
                    color: simResult.delta.peakRisk < 0 ? '#34d399' : '#f87171',
                  }}
                >
                  {simResult.delta.peakRisk <= 0
                    ? `${(simResult.delta.peakRisk * 100).toFixed(0)}%`
                    : `+${(simResult.delta.peakRisk * 100).toFixed(0)}%`}
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Clearance: -120s</div>
              </div>
            </div>

            {/* Recommendation Summary Text */}
            <p style={{ fontSize: '0.8rem', color: '#e2e8f0', lineHeight: 1.35 }}>
              {simResult.recommendation}
            </p>

            {/* Transferred Risk Callout Banner */}
            {simResult.riskTransferNotes && simResult.riskTransferNotes.length > 0 && (
              <div
                style={{
                  background: 'rgba(234, 179, 8, 0.1)',
                  border: '1px solid rgba(234, 179, 8, 0.35)',
                  borderRadius: '4px',
                  padding: '8px 10px',
                }}
              >
                <div
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: '#fbbf24',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    marginBottom: '4px',
                  }}
                >
                  <AlertTriangle size={13} />
                  Transferred Upstream Risk Warnings:
                </div>
                <ul style={{ paddingLeft: '16px', fontSize: '0.72rem', color: '#fef3c7' }}>
                  {simResult.riskTransferNotes.map((note, i) => (
                    <li key={i}>{note}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Per-Zone Impact Details */}
            {simResult.zoneImpacts && simResult.zoneImpacts.length > 0 && (
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                  PER-SECTOR RISK COMPARISON
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  {simResult.zoneImpacts.map((zi) => (
                    <div
                      key={zi.zoneId}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.75rem',
                        background: 'rgba(0, 0, 0, 0.2)',
                        padding: '4px 8px',
                        borderRadius: '4px',
                      }}
                    >
                      <span className="mono" style={{ color: '#ffffff' }}>
                        {zi.zoneId}
                      </span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span className="mono" style={{ color: 'var(--text-muted)' }}>
                          {zi.baselineScore.toFixed(2)} → {zi.projectedScore.toFixed(2)}
                        </span>
                        <span
                          className={`badge ${
                            zi.status === 'IMPROVED'
                              ? 'badge-low'
                              : zi.status === 'TRANSFERRED_RISK'
                              ? 'badge-moderate'
                              : 'badge-advisory'
                          }`}
                          style={{ fontSize: '0.65rem', padding: '1px 5px' }}
                        >
                          {zi.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
