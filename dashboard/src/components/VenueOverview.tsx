import React from 'react';
import { CurrentAnalyticsResponse, RiskResponse, RiskLevel } from '../types';
import { Layers, AlertCircle, ArrowUpDown, Users, Gauge } from 'lucide-react';

interface VenueOverviewProps {
  analytics: CurrentAnalyticsResponse | null;
  risk: RiskResponse | null;
  selectedZoneId: string;
  onSelectZone: (zoneId: string) => void;
}

const ZONE_FRIENDLY_NAMES: Record<string, string> = {
  'z-entry-a': 'Entry Gate A',
  'z-corridor-1': 'Transit Corridor 1',
  'z-open-yard': 'Central Open Yard',
  'z-exit-east': 'East Egress Exit',
};

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

function getCardBorder(level?: RiskLevel, isSelected?: boolean): string {
  if (isSelected) return '2px solid #38bdf8';
  switch (level) {
    case 'CRITICAL':
      return '1px solid rgba(239, 68, 68, 0.6)';
    case 'HIGH':
      return '1px solid rgba(249, 115, 22, 0.6)';
    case 'MODERATE':
      return '1px solid rgba(234, 179, 8, 0.5)';
    default:
      return '1px solid var(--border-color)';
  }
}

export const VenueOverview: React.FC<VenueOverviewProps> = ({
  analytics,
  risk,
  selectedZoneId,
  onSelectZone,
}) => {
  const zones = analytics?.zones ?? [];
  const zoneRiskMap = new Map(risk?.zoneRisks?.map((zr) => [zr.zoneId, zr]));

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-title">
          <Layers size={17} color="#38bdf8" />
          Venue Zone Overview ({zones.length} Sectors)
        </span>
        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          Click sector to inspect real-time diagnostics
        </span>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '12px',
        }}
      >
        {zones.map((zone) => {
          const zoneRisk = zoneRiskMap.get(zone.zoneId);
          const level = zoneRisk?.level ?? zone.bottleneck.severity ?? 'LOW';
          const score = zoneRisk?.score ?? 0.1;
          const isSelected = zone.zoneId === selectedZoneId;
          const friendlyName = ZONE_FRIENDLY_NAMES[zone.zoneId] || zone.zoneId;

          return (
            <div
              key={zone.zoneId}
              onClick={() => onSelectZone(zone.zoneId)}
              style={{
                background: isSelected ? '#172338' : 'var(--bg-subtle)',
                border: getCardBorder(level, isSelected),
                borderRadius: '8px',
                padding: '14px',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
                display: 'flex',
                flexDirection: 'column',
                gap: '10px',
                position: 'relative',
              }}
            >
              {/* Header row */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>{friendlyName}</h3>
                  <span className="mono" style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {zone.zoneId}
                  </span>
                </div>
                <span className={`badge ${getBadgeClass(level)}`}>
                  {level} ({(score * 100).toFixed(0)}%)
                </span>
              </div>

              {/* Core metrics */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '8px',
                  fontSize: '0.8rem',
                }}
              >
                <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '4px' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <Users size={11} /> Density
                  </div>
                  <strong style={{ fontSize: '0.85rem' }}>{zone.densityPerSqM.toFixed(1)} /m²</strong>
                </div>

                <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '4px' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <ArrowUpDown size={11} /> Net Flow
                  </div>
                  <strong style={{ fontSize: '0.85rem', color: zone.netPressure > 20 ? '#f97316' : 'inherit' }}>
                    {zone.netPressure >= 0 ? `+${zone.netPressure}` : zone.netPressure} /m
                  </strong>
                </div>

                <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '4px' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>Queue Length</div>
                  <strong style={{ fontSize: '0.85rem' }}>{zone.queueLength} pers</strong>
                </div>

                <div style={{ background: 'rgba(0,0,0,0.2)', padding: '6px 8px', borderRadius: '4px' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <Gauge size={11} /> Speed Drop
                  </div>
                  <strong style={{ fontSize: '0.85rem', color: zone.speedDropPct > 35 ? '#ef4444' : 'inherit' }}>
                    {zone.speedDropPct.toFixed(0)}%
                  </strong>
                </div>
              </div>

              {/* Status alerts/badges */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '2px' }}>
                {zone.bottleneck.isBottleneck && (
                  <span
                    style={{
                      fontSize: '0.68rem',
                      padding: '2px 5px',
                      borderRadius: '3px',
                      background: 'rgba(239, 68, 68, 0.2)',
                      color: '#f87171',
                      border: '1px solid rgba(239, 68, 68, 0.4)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '3px',
                    }}
                  >
                    <AlertCircle size={10} /> BOTTLENECK CHOKE
                  </span>
                )}
                {zone.counterflow.isCounterflowDetected && (
                  <span
                    style={{
                      fontSize: '0.68rem',
                      padding: '2px 5px',
                      borderRadius: '3px',
                      background: 'rgba(249, 115, 22, 0.2)',
                      color: '#fb923c',
                      border: '1px solid rgba(249, 115, 22, 0.4)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '3px',
                    }}
                  >
                    <ArrowUpDown size={10} /> COUNTERFLOW CONFLICT
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
