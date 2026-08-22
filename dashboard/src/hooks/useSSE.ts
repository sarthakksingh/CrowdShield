import { useEffect, useRef } from 'react';
import { API_BASE_URL } from '../api/client';

export function useSSE(onMessage: () => void) {
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    let active = true;

    function connect() {
      if (!active) return;
      try {
        const streamUrl = `${API_BASE_URL}/api/stream`;
        const es = new EventSource(streamUrl);
        eventSourceRef.current = es;

        es.addEventListener('zone_metrics', () => {
          if (active) onMessage();
        });

        es.addEventListener('risk_update', () => {
          if (active) onMessage();
        });

        es.addEventListener('alert_raised', () => {
          if (active) onMessage();
        });

        es.onmessage = () => {
          if (active) onMessage();
        };

        es.onerror = () => {
          es.close();
          if (active) {
            // Reconnect after 3 seconds
            setTimeout(connect, 3000);
          }
        };
      } catch (err) {
        if (active) {
          setTimeout(connect, 3000);
        }
      }
    }

    connect();

    return () => {
      active = false;
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [onMessage]);
}
