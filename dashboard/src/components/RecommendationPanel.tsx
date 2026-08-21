import React from 'react';
import { RecommendationsResponse, Recommendation, RiskLevel } from '../types';
import { Lightbulb, CheckCheck, PlayCircle, ShieldCheck } from 'lucide-react';

interface RecommendationPanelProps {
  recommendationsResponse: RecommendationsResponse | null;
  onSelectForSimulation: (rec: Recommendation) => void;
}

function getUrgencyBadge(urgency: RiskLevel): string {
  switch (urgency) {
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

function getActionTypeColor(actionType: string): string {
  switch (actionType) {
    case 'RESTRICT_GATE':
      return '#ef4444';
    case 'OPEN_EXIT':
    case 'OPEN_ROUTE':
      return '#10b981';
    case 'REDIRECT_INFLOW':
      return '#f97316';
    case 'DEPLOY_PERSONNEL':
      return '#a855f7';
    case 'BROADCAST_ALERT':
      return '#38bdf8';
    default:
      return '#38bdf8';
  }
}

export const RecommendationPanel: React.FC<RecommendationPanelProps> = ({
  recommendationsResponse,
  onSelectForSimulation,
}) => {
  const recommendations = recommendationsResponse?.recommendations ?? [];
  const advisoryNotice = recommendationsResponse?.advisoryNotice;

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="card-header">
        <span className="card-title">
          <Lightbulb size={17} color="#38bdf8" />
          Actionable Advisory Recommendations ({recommendations.length})
        </span>
        <span className="badge badge-advisory">DECISION-SUPPORT ONLY</span>
      </div>

      {advisoryNotice && (
        <div
          style={{
            fontSize: '0.72rem',
            color: 'var(--text-muted)',
            marginBottom: '10px',
            fontStyle: 'italic',
            borderBottom: '1px dashed var(--border-color)',
            paddingBottom: '6px',
          }}
        >
          {advisoryNotice}
        </div>
      )}

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          overflowY: 'auto',
          maxHeight: '380px',
          paddingRight: '4px',
        }}
      >
        {recommendations.length === 0 ? (
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
            <ShieldCheck size={24} color="#10b981" />
            <div style={{ fontSize: '0.85rem', color: '#10b981', fontWeight: 600 }}>
              No Urgent Interventions Required
            </div>
            <div style={{ fontSize: '0.75rem' }}>
              Crowd flow is currently laminar and within normal venue capacity thresholds.
            </div>
          </div>
        ) : (
          recommendations.map((rec) => {
            const actionColor = getActionTypeColor(rec.actionType);

            return (
              <div
                key={rec.id}
                style={{
                  background: '#131b2e',
                  border: '1px solid var(--border-color)',
                  borderRadius: '6px',
                  padding: '12px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                  transition: 'border-color 0.15s ease',
                }}
              >
                {/* Header with Rank & Badges */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span
                      style={{
                        background: 'var(--bg-subtle)',
                        color: 'var(--text-secondary)',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        padding: '2px 6px',
                        borderRadius: '4px',
                      }}
                    >
                      #{rec.rank}
                    </span>
                    <span
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '4px',
                        background: `${actionColor}22`,
                        color: actionColor,
                        border: `1px solid ${actionColor}55`,
                      }}
                    >
                      {rec.actionType}
                    </span>
                    <span className="mono" style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Target: {rec.targetZoneId}
                    </span>
                  </div>

                  <span className={`badge ${getUrgencyBadge(rec.urgency)}`}>{rec.urgency}</span>
                </div>

                {/* Title & Reason */}
                <div>
                  <h4 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#f8fafc', marginBottom: '3px' }}>
                    {rec.title}
                  </h4>
                  <p style={{ fontSize: '0.78rem', color: '#cbd5e1', lineHeight: 1.35 }}>{rec.reason}</p>
                </div>

                {/* Expected Impact */}
                <div
                  style={{
                    background: 'rgba(56, 189, 248, 0.06)',
                    border: '1px solid rgba(56, 189, 248, 0.2)',
                    borderRadius: '4px',
                    padding: '6px 8px',
                    fontSize: '0.75rem',
                    color: '#7dd3fc',
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '6px',
                  }}
                >
                  <CheckCheck size={14} style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <strong>Expected Impact:</strong> {rec.expectedImpact}
                  </div>
                </div>

                {/* Suggested Message for PA/Broadcast */}
                {rec.suggestedMessage && (
                  <div
                    style={{
                      background: 'rgba(0, 0, 0, 0.25)',
                      borderRadius: '4px',
                      padding: '6px 8px',
                      fontSize: '0.72rem',
                      color: 'var(--text-muted)',
                      fontStyle: 'italic',
                    }}
                  >
                    "{rec.suggestedMessage}"
                  </div>
                )}

                {/* Footer Action */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '2px' }}>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                    Confidence: {(rec.confidence * 100).toFixed(0)}%
                  </span>

                  {rec.actionType !== 'BROADCAST_ALERT' && (
                    <button
                      className="btn btn-secondary"
                      onClick={() => onSelectForSimulation(rec)}
                      style={{ fontSize: '0.72rem', padding: '3px 8px' }}
                    >
                      <PlayCircle size={13} color="#38bdf8" />
                      <span>Test in Simulator</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
