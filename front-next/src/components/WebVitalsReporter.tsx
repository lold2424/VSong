'use client';

import { useEffect } from 'react';
import reportWebVitals from '@/utils/web-vitals';

export function WebVitalsReporter() {
  useEffect(() => {
    reportWebVitals(metric => {
    });
  }, []);

  return null;
}
