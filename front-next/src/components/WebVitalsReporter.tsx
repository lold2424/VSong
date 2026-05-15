'use client';

import { useEffect } from 'react';
import reportWebVitals from '@/utils/web-vitals';
import { apiClient } from '@/utils/apiClient';

export function WebVitalsReporter() {
  useEffect(() => {
    const trackVisit = async () => {
      try {
        await apiClient.post('/track-visit');
      } catch {
      }
    };

    trackVisit();

    reportWebVitals(() => {
    });
  }, []);

  return null;
}
