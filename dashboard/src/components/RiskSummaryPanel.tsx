import React from 'react';
import { RiskResponse, RiskLevel, ForecastResponse } from '../types';
import { Activity, TrendingUp, TrendingDown, Minus, Clock, Zap, Compass } from 'lucide-react';

interface RiskSummaryPanelProps {
  risk: RiskResponse | null;
  forecast?: ForecastResponse | null;
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

export const RiskSummaryPanel: React.FC<RiskSummaryPanelProps> = ({ risk, forecast }) => {
  const overall = risk?.overallRisk;
  const score = overall?.score ?? 0.0;
  const level = overall?.level ?? 'LOW';
  const trend = overall?.trend ?? 'STABLE';
  const horizon = overall?.horizon ?? 'nominal';
  const contributions = overall?.contributions ?? {};
  const factorDescriptions = overall?.factorDescriptions ?? {};

  const overallForecast = forecast?.overallForecast;
  const methodsAgree = forecast?.zoneForecasts ? forecast.zoneForecasts.every((z) => z.methodsAgree) : true;

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
          marginBottom: '14px',
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

      {/* Short-Horizon Statistical Projection */}
      {overallForecast && (
        <div
          style={{
            background: 'var(--bg-subtle)',
            borderRadius: '8px',
            padding: '10px 14px',
            marginBottom: '14px',
            border: '1px solid var(--border-color)',
            opacity: methodsAgree ? 1.0 : 0.85,
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '8px',
            }}
          >
            <div
              style={{
                fontSize: '0.75rem',
                fontWeight: 600,
                color: 'var(--text-secondary)',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <Compass size={13} color="#38bdf8" />
              <span>STATISTICAL PROJECTION (Linear + Exp Smoothing)</span>
            </div>
            <div
              style={{
                fontSize: '0.72rem',
                color: 'var(--text-muted)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <span>Confidence: <strong>{Math.round((overallForecast.confidence ?? 0.85) * 100)}%</strong></span>
              {!methodsAgree && (
                <span
                  style={{
                    color: '#f97316',
                    fontSize: '0.68rem',
                    background: 'rgba(249, 115, 22, 0.15)',
                    padding: '1px 5px',
                    borderRadius: '4px',
                  }}
                  title="Models diverge; projection dampened"
                >
                  Divergence Damped
                </span>
              )}
            </div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(3, 1fr)',
              gap: '8px',
              textAlign: 'center',
            }}
          >
            {/* Trajectory */}
            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '6px' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>PROJECTED TREND</div>
              <div
                style={{
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  color: overallForecast.trend === 'RISING' ? '#ef4444' : overallForecast.trend === 'FALLING' ? '#10b981' : 'var(--text-secondary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '3px',
                  marginTop: '2px',
                }}
              >
                {overallForecast.trend === 'RISING' && <TrendingUp size={13} />}
                {overallForecast.trend === 'FALLING' && <TrendingDown size={13} />}
                {overallForecast.trend === 'STABLE' && <Minus size={13} />}
                <span>{overallForecast.trend}</span>
              </div>
            </div>

            {/* +60s Horizon */}
            {overallForecast.horizons?.['60'] && (
              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '6px' }}>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>+60s HORIZON</div>
                <div
                  className="mono"
                  style={{
                    fontSize: '0.88rem',
                    fontWeight: 700,
                    color: getScoreColor(overallForecast.horizons['60'].score),
                    marginTop: '2px',
                  }}
                >
                  {overallForecast.horizons['60'].score.toFixed(2)}
                  <span style={{ fontSize: '0.68rem', marginLeft: '4px', fontWeight: 600 }}>
                    ({overallForecast.horizons['60'].level})
                  </span>
                </div>
              </div>
            )}

            {/* +180s Horizon */}
            {overallForecast.horizons?.['180'] && (
              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '6px' }}>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>+180s HORIZON</div>
                <div
                  className="mono"
                  style={{
                    fontSize: '0.88rem',
                    fontWeight: 700,
                    color: getScoreColor(overallForecast.horizons['180'].score),
                    marginTop: '2px',
                  }}
                >
                  {overallForecast.horizons['180'].score.toFixed(2)}
                  <span style={{ fontSize: '0.68rem', marginLeft: '4px', fontWeight: 600 }}>
                    ({overallForecast.horizons['180'].level})
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

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
