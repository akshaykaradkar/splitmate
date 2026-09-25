/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {setupRipple} from '@material/web/labs/gb/components/ripple/ripple';
import rippleStyles from '@material/web/labs/gb/components/ripple/ripple.css' with {type: 'css'};
import {adoptStyles} from '@material/web/labs/gb/styles/adopt-styles';
import {type RefObject, useEffect} from 'react';

/** Hook for useRipple behavior and styling. */
export function useRipple(ref: RefObject<HTMLElement | null>): void {
  useEffect(() => {
    if (!ref.current) return;
    const cleanup = new AbortController();
    setupRipple(ref.current, {signal: cleanup.signal});
    adoptStyles(ref.current, rippleStyles);
    return () => cleanup.abort();
  }, [ref]);
}
