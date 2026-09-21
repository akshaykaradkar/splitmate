/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
// TODO: use gBreeze divider once tabs component no longer depends on MWC's 
// divider. This will avoid "md-divider has already been defined" errors.
import { MdDivider as DividerElement } from '@material/web/divider/divider';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-divider': WebComponentProps<DividerElement>;
    }
  }
}

const DividerWebComponent = createComponent({
  tagName: 'md-divider',
  elementClass: DividerElement,
  react: React,
});

export interface DividerProps extends React.ComponentProps<
  typeof DividerWebComponent
> {
  orientation?: 'horizontal' | 'vertical';
  inset?: boolean;
  insetStart?: boolean;
  insetEnd?: boolean;
}

export function Divider({
  orientation,
  inset,
  insetStart,
  insetEnd,
  style = {},
  ...props
}: DividerProps) {
  const isVertical = orientation === 'vertical';
  if (isVertical) {
    props['aria-orientation'] = 'vertical';
    // TODO: Set props.vertical once replacing with gBreeze divider
    // props.vertical = true;
    style = {
      width: 'var(--md-divider-thickness,1px)',
      height: '100%',
      ...style,
    };
  }

  // Add padding for inset variants
  style = {
    paddingInlineEnd:
      !isVertical && (inset || insetEnd) ? '16px' : undefined,
    paddingInlineStart:
      !isVertical && (inset || insetStart) ? '16px' : undefined,
    paddingBlockEnd:
      isVertical && (inset || insetEnd) ? '16px' : undefined,
    paddingBlockStart:
      isVertical && (inset || insetStart) ? '16px' : undefined,
    ...style,
  };

  return <DividerWebComponent style={style} {...props} />;
}
