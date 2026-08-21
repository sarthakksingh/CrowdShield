import React from 'react';
import { RiskResponse, RiskLevel } from '../types';
import { Activity, TrendingUp, TrendingDown, Minus, Clock, Zap } from 'lucide-react';

interface RiskSummaryPanelProps {
  risk: RiskResponse | null;
}

function getBadgeClass(level?: RiskLevel): string {
  switch (level) {
    case 'CRITICAL':
      return 'badge-critical';
    case 'HIGH':
      return 'badge-high';
    case 'MODERATE':
      return 'badge-moderate';
    default:
      return 'badge-low';
  }
}

function getScoreColor(score: number): string {
  if (score >= 0.75) return '#ef4444';
  if (score >= 0.55) return '#f97316';
  if (score >= 0.35) return '#eab308';
  return '#10b981';
}

const FACTOR_NAMES: Record<string, string> = {
  density_pressure: 'Density Pressure',
  inflow_outflow_imbalance: 'Inflow/Outflow Imbalance',
  movement_instability: 'Movement Instability',
  opposing_movement: 'Opposing Flow / Turbulence',
  bottleneck_pressure: 'Bottleneck Resistance',
  route_availability: 'Route Availability Deficit',
};

export const RiskSummaryPanel: React.FC<RiskSummaryPanelProps> = ({ risk }) => {
  const overall = risk?.overallRisk;
  const score = overall?.score ?? 0.0;
  const level = overall?.level ?? 'LOW';
  const trend = overall?.trend ?? 'STABLE';
  const horizon = overall?.horizon ?? 'nominal';
  const contributions = overall?.contributions ?? {};
  const factorDescriptions = overall?.factorDescriptions ?? {};

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">
          <Activity size={17} color="#38bdf8" />
          Overall Risk Engine Assessment
        </span>
        <span className={`badge ${getBadgeClass(level)}`}>{level}</span>
      </div>

      {/* Main Score & Horizon Hero Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '12px',
          marginBottom: '16px',
        }}
      >
        {/* Score Card */}
        <div
          style={{
            background: 'var(--bg-subtle)',
            borderRadius: '8px',
            padding: '12px 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            border: `1px solid ${getScoreColor(score)}44`,
          }}
        >
          <div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Risk Index
            </div>
            <div
              className="mono"
              style={{
                fontSize: '2rem',
                fontWeight: 800,
                color: getScoreColor(score),
                lineHeight: 1.1,
              }}
            >
              {score.toFixed(2)}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Confidence: {((overall?.confidence ?? 0.85) * 100).toFixed(0)}%
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>TREND</div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                fontSize: '0.85rem',
                fontWeight: 700,
                color: trend === 'RISING' ? '#ef4444' : trend === 'FALLING' ? '#10b981' : 'var(--text-secondary)',
              }}
            >
              {trend === 'RISING' && <TrendingUp size={16} />}
              {trend === 'FALLING' && <TrendingDown size={16} />}
              {trend === 'STABLE' && <Minus size={16} />}
              <span>{trend}</span>
            </div>
          </div>
        </div>

        {/* Horizon Card */}
        <div
          style={{
            background: 'var(--bg-subtle)',
            borderRadius: '8px',
            padding: '12px 16px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            border: '1px solid var(--border-color)',
          }}
        >
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> TIME-TO-CRITICAL HORIZON
          </div>
          <div style={{ fontSize: '1.15rem', fontWeight: 700, color: '#f8fafc', margin: '4px 0' }}>
            {horizon}
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            Dynamic estimate based on current velocity & inflow rate
          </div>
        </div>
      </div>

      {/* Factor Contribution Drivers */}
      <div>
        <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '4px' }}>
          <Zap size={14} color="#38bdf8" />
          Primary Risk Driving Factors (% Impact)
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {Object.entries(contributions).map(([factorKey, weightVal]) => {
            const pct = Math.round(weightVal * 100);
            const label = FACTOR_NAMES[factorKey] || factorKey;
            const explanation = factorDescriptions[factorKey];

            return (
              <div key={factorKey} style={{ background: 'rgba(0,0,0,0.15)', padding: '6px 10px', borderRadius: '6px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '3px' }}>
                  <span style={{ color: '#e2e8f0', fontWeight: 500 }}>{label}</span>
                  <span className="mono" style={{ fontWeight: 700, color: '#38bdf8' }}>{pct}%</span>
                </div>
                <div style={{ height: '5px', background: 'var(--bg-subtle)', borderRadius: '3px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${Math.min(100, pct * 2.5)}%`,
                      background: pct > 25 ? '#ef4444' : pct > 15 ? '#f97316' : '#38bdf8',
                      borderRadius: '3px',
                    }}
                  />
                </div>
                {explanation && (
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '3px' }}>
                    {explanation}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
