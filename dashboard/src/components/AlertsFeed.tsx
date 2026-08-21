import React from 'react';
import { AlertRecord } from '../types';
import { Bell, ShieldAlert, CheckCircle2 } from 'lucide-react';

interface AlertsFeedProps {
  alerts: AlertRecord[];
}

function getAlertClass(severity: string): string {
  switch (severity.toUpperCase()) {
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

export const AlertsFeed: React.FC<AlertsFeedProps> = ({ alerts }) => {
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="card-header">
        <span className="card-title">
          <Bell size={17} color="#f97316" />
          Active Incident Alerts ({alerts.length})
        </span>
        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          Persistence filtered & deduped
        </span>
      </div>

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
          overflowY: 'auto',
          maxHeight: '320px',
          paddingRight: '4px',
        }}
      >
        {alerts.length === 0 ? (
          <div
            style={{
              padding: '24px 16px',
              textAlign: 'center',
              color: 'var(--text-muted)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <CheckCircle2 size={24} color="#10b981" />
            <div style={{ fontSize: '0.85rem', color: '#10b981', fontWeight: 600 }}>All Zones Nominal</div>
            <div style={{ fontSize: '0.75rem' }}>No sustained critical risk conditions detected.</div>
          </div>
        ) : (
          alerts.map((alert) => (
            <div
              key={alert.id}
              style={{
                background: 'var(--bg-subtle)',
                borderLeft: `4px solid ${
                  alert.severity === 'CRITICAL' ? '#ef4444' : alert.severity === 'HIGH' ? '#f97316' : '#eab308'
                }`,
                borderRadius: '4px',
                padding: '8px 12px',
                display: 'flex',
                flexDirection: 'column',
                gap: '4px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <ShieldAlert size={14} color={alert.severity === 'CRITICAL' ? '#ef4444' : '#f97316'} />
                  <span className="mono" style={{ fontSize: '0.8rem', fontWeight: 700, color: '#ffffff' }}>
                    {alert.zoneId}
                  </span>
                </div>
                <span className={`badge ${getAlertClass(alert.severity)}`}>{alert.severity}</span>
              </div>

              <div style={{ fontSize: '0.8rem', color: '#e2e8f0', lineHeight: 1.3 }}>{alert.message}</div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  fontSize: '0.68rem',
                  color: 'var(--text-muted)',
                  marginTop: '2px',
                }}
              >
                <span>Key: {alert.dedupeKey}</span>
                <span className="mono">{alert.timestamp.substring(11, 19)} UTC</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
