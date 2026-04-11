import {
  onCLS, onINP, onLCP, onFCP, onTTFB,
  type CLSMetric, type INPMetric, type LCPMetric, type FCPMetric, type TTFBMetric
} from 'web-vitals';

export type WebVitalsMetric = CLSMetric | INPMetric | LCPMetric | FCPMetric | TTFBMetric;

const reportWebVitals = (onReport?: (metric: WebVitalsMetric) => void) => {
  const sendToAnalytics = (metric: WebVitalsMetric) => {
    if (metric.value === undefined) {
      return;
    }

    const body = JSON.stringify(metric);
    const url = '/api/vitals';

    if (navigator.sendBeacon) {
      navigator.sendBeacon(url, body);
    } else {
      fetch(url, {
        body,
        method: 'POST',
        credentials: 'omit',
        headers: {
          'Content-Type': 'application/json',
        },
      });
    }

    if (onReport) {
      onReport(metric);
    }
  };

  onCLS(sendToAnalytics);
  onINP(sendToAnalytics);
  onLCP(sendToAnalytics);
  onFCP(sendToAnalytics);
  onTTFB(sendToAnalytics);
};

export default reportWebVitals;
