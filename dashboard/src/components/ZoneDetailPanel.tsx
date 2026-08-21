import React, { useEffect, useState, useCallback } from 'react';
import { ZoneAnalyticsDetailResponse } from '../types';
import { api } from '../api/client';
import { BarChart3, AlertOctagon, RefreshCw, Compass, TrendingUp, AlertTriangle } from 'lucide-react';

interface ZoneDetailPanelProps {
  zoneId: string;
  refreshTrigger?: number;
}

export const ZoneDetailPanel: React.FC<ZoneDetailPanelProps> = ({ zoneId, refreshTrigger }) => {
  const [detail, setDetail] = useState<ZoneAnalyticsDetailResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchZoneAnalytics = useCallback(async (targetId: string) => {
    if (!targetId || targetId.trim() === '') {
      setDetail(null);
      setError('No sector selected');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await api.getZoneAnalytics(targetId);
      setDetail(data);
    } catch (err: any) {
      console.error(`Failed to load zone analytics for ${targetId}:`, err);
      setError(err.message || `Failed to fetch analytics for sector ${targetId}`);
      setDetail(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchZoneAnalytics(zoneId);
  }, [zoneId, refreshTrigger, fetchZoneAnalytics]);

  const current = detail?.analytics || detail?.current;
  const history = detail?.timeline || detail?.timelineHistory || [];

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">
          <BarChart3 size={17} color="#38bdf8" />
          Sector Diagnostics: <span className="mono" style={{ color: '#ffffff' }}>{zoneId || 'None'}</span>
        </span>
        <button
          className="btn btn-secondary"
          onClick={() => fetchZoneAnalytics(zoneId)}
          disabled={loading || !zoneId}
          style={{ padding: '2px 8px', fontSize: '0.72rem' }}
          title="Refresh Diagnostics"
        >
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} color="var(--text-muted)" />
          <span>Refresh</span>
        </button>
      </div>

      {loading && !detail ? (
        <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
          <RefreshCw size={20} className="animate-spin" style={{ margin: '0 auto 8px', display: 'block' }} />
          Loading diagnostics for sector <strong className="mono">{zoneId}</strong>...
        </div>
      ) : error ? (
        <div
          style={{
            padding: '16px',
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '6px',
            color: '#f87171',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
            <AlertTriangle size={16} />
            <span>Diagnostics Error: {error}</span>
          </div>
          <button
            className="btn btn-secondary"
            onClick={() => fetchZoneAnalytics(zoneId)}
            style={{ width: 'fit-content', padding: '4px 10px', fontSize: '0.75rem' }}
          >
            Retry Fetch
          </button>
        </div>
      ) : !current ? (
        <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
          Select a sector above to view detailed diagnostics.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Diagnostic Metrics Matrix */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
              gap: '8px',
            }}
          >
            <div style={{ background: 'var(--bg-subtle)', padding: '8px 10px', borderRadius: '6px' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>DENSITY</div>
              <div style={{ fontSize: '1rem', fontWeight: 700 }}>
                {current.densityPerSqM.toFixed(1)} <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>/m²</span>
              </div>
              <div style={{ fontSize: '0.68rem', color: current.densityTrend === 'RISING' ? '#ef4444' : '#10b981' }}>
                Trend: {current.densityTrend}
              </div>
            </div>

            <div style={{ background: 'var(--bg-subtle)', padding: '8px 10px', borderRadius: '6px' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>INFLOW / OUTFLOW</div>
              <div style={{ fontSize: '0.95rem', fontWeight: 700 }}>
                {current.inflowPerMin} / {current.outflowPerMin}
              </div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                Net: <strong style={{ color: current.netPressure > 0 ? '#f97316' : '#10b981' }}>+{current.netPressure}/m</strong>
              </div>
            </div>

            <div style={{ background: 'var(--bg-subtle)', padding: '8px 10px', borderRadius: '6px' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>QUEUE LENGTH</div>
              <div style={{ fontSize: '1rem', fontWeight: 700 }}>
                {current.queueLength} <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>pers</span>
              </div>
              <div style={{ fontSize: '0.68rem', color: current.queueGrowth > 0 ? '#f97316' : 'var(--text-muted)' }}>
                Growth: {current.queueGrowth >= 0 ? `+${current.queueGrowth}` : current.queueGrowth}
              </div>
            </div>

            <div style={{ background: 'var(--bg-subtle)', padding: '8px 10px', borderRadius: '6px' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>SPEED DEGRADATION</div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: current.speedDropPct > 35 ? '#ef4444' : 'inherit' }}>
                {current.speedDropPct.toFixed(0)}%
              </div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                Instability: {(current.movementInstability * 100).toFixed(0)}%
              </div>
            </div>
          </div>

          {/* Explainability Callouts */}
          {current.bottleneck.isBottleneck && (
            <div
              style={{
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                borderRadius: '6px',
                padding: '10px 12px',
              }}
            >
              <div
                style={{
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  color: '#f87171',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  marginBottom: '4px',
                }}
              >
                <AlertOctagon size={14} />
                Bottleneck Risk Factors Detected ({current.bottleneck.severity})
              </div>
              <ul style={{ paddingLeft: '18px', fontSize: '0.75rem', color: '#e2e8f0' }}>
                {current.bottleneck.reasons.map((r, i) => (
                  <li key={i}>{r.replace(/_/g, ' ')}</li>
                ))}
              </ul>
            </div>
          )}

          {current.counterflow.isCounterflowDetected && (
            <div
              style={{
                background: 'rgba(249, 115, 22, 0.1)',
                border: '1px solid rgba(249, 115, 22, 0.3)',
                borderRadius: '6px',
                padding: '10px 12px',
              }}
            >
              <div
                style={{
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  color: '#fb923c',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  marginBottom: '4px',
                }}
              >
                <Compass size={14} />
                Counterflow Turbulence Detected (Opposing: {current.counterflow.opposingMovementScore.toFixed(2)})
              </div>
              <ul style={{ paddingLeft: '18px', fontSize: '0.75rem', color: '#e2e8f0' }}>
                {current.counterflow.reasons.map((r, i) => (
                  <li key={i}>{r.replace(/_/g, ' ')}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Timeline History Progression */}
          {history.length > 0 && (
            <div>
              <div
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: 'var(--text-muted)',
                  marginBottom: '6px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <TrendingUp size={13} /> TIMELINE EVOLUTION (Past {history.length} frames)
              </div>
              <div style={{ maxHeight: '140px', overflowY: 'auto', border: '1px solid var(--border-color)', borderRadius: '6px' }}>
                <table style={{ width: '100%', fontSize: '0.72rem', borderCollapse: 'collapse' }}>
                  <thead style={{ background: 'var(--bg-subtle)', color: 'var(--text-muted)' }}>
                    <tr>
                      <th style={{ padding: '4px 6px', textAlign: 'left' }}>t(s)</th>
                      <th style={{ padding: '4px 6px', textAlign: 'right' }}>Density</th>
                      <th style={{ padding: '4px 6px', textAlign: 'right' }}>In / Out</th>
                      <th style={{ padding: '4px 6px', textAlign: 'right' }}>Queue</th>
                      <th style={{ padding: '4px 6px', textAlign: 'right' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((pt, i) => (
                      <tr
                        key={i}
                        style={{
                          borderTop: '1px solid var(--border-color)',
                          background: pt.offsetSec === detail.currentOffsetSec ? 'rgba(56, 189, 248, 0.1)' : undefined,
                        }}
                      >
                        <td className="mono" style={{ padding: '3px 6px' }}>
                          +{pt.offsetSec}s
                        </td>
                        <td className="mono" style={{ padding: '3px 6px', textAlign: 'right' }}>
                          {pt.densityPerSqM.toFixed(1)}
                        </td>
                        <td className="mono" style={{ padding: '3px 6px', textAlign: 'right' }}>
                          {pt.inflowPerMin}/{pt.outflowPerMin}
                        </td>
                        <td className="mono" style={{ padding: '3px 6px', textAlign: 'right' }}>
                          {pt.queueLength}
                        </td>
                        <td style={{ padding: '3px 6px', textAlign: 'right' }}>
                          {pt.isBottleneck ? (
                            <span style={{ color: '#ef4444', fontWeight: 600 }}>BOTTLENECK</span>
                          ) : pt.isCounterflow ? (
                            <span style={{ color: '#f97316', fontWeight: 600 }}>COUNTERFLOW</span>
                          ) : (
                            <span style={{ color: '#10b981' }}>NOMINAL</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
