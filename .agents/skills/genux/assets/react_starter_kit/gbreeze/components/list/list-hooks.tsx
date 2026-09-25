/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import type {
  ListClassesState,
  ListItemClassesState,
} from '@material/web/labs/gb/components/list/list.js';
import {
  listClasses,
  listItemClasses,
  setupListItem,
} from '@material/web/labs/gb/components/list/list.js';
import {adoptStyles} from '@material/web/labs/gb/styles/adopt-styles.js';
import React, {useEffect} from 'react';

import focusRingStyles from '@material/web/labs/gb/components/focus/focus-ring.css' with {type: 'css'};
import listStyles from '@material/web/labs/gb/components/list/list.css' with {type: 'css'};
import rippleStyles from '@material/web/labs/gb/components/ripple/ripple.css' with {type: 'css'};

// Helper to convert Lit ClassInfo to string
function classInfoToString(classInfo: Record<string, unknown>): string {
  return Object.entries(classInfo)
    .filter(([, value]) => value)
    .map(([key]) => key)
    .join(' ');
}

/** Hook to apply Material 3 list styles and classes to a list container ref. */
export function useList(
  ref: React.RefObject<HTMLElement | null>,
  state?: ListClassesState,
) {
  const classes = classInfoToString(listClasses(state));

  useEffect(() => {
    if (ref.current) {
      adoptStyles(ref.current, listStyles);
    }
  }, [ref]);

  return classes;
}

/** Hook to apply Material 3 list item styles, classes, and interactive behaviors to a list item ref. */
export function useListItem(
  ref: React.RefObject<HTMLElement | null>,
  state?: ListItemClassesState,
) {
  const classes = classInfoToString(listItemClasses(state));

  useEffect(() => {
    if (ref.current) {
      const controller = new AbortController();
      setupListItem(ref.current, {signal: controller.signal});
      adoptStyles(ref.current, [focusRingStyles, rippleStyles, listStyles]);
      return () => controller.abort();
    }
  }, [ref]);

  return classes;
}
