/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {BadgeElement} from '@material/web/labs/gb/components/badge/badge-element.js';
import '@material/web/labs/gb/components/badge/md-gb-badge.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-badge': WebComponentProps<BadgeElement>;
    }
  }
}

const BadgeWebComponent = createComponent({
  tagName: 'md-gb-badge',
  elementClass: BadgeElement,
  react: React,
});

/** Props for the Badge component. */
export interface BadgeProps
  extends Omit<React.ComponentProps<typeof BadgeWebComponent>, 'children'> {
  value?: React.ReactNode;
  dot?: boolean;
  children?: React.ReactNode;
}

/** Material 3 Badge component. */
export function Badge({value, dot, children, ...props}: BadgeProps) {
  const content = dot ? null : (value ?? children);
  return <BadgeWebComponent {...props}>{content}</BadgeWebComponent>;
}
