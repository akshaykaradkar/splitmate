/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import type {WebComponentProps} from '@lit/react';
import type {IconElement} from '@material/web/labs/gb/styles/icon/icon-element.js';
import '@material/web/labs/gb/styles/icon/md-gb-icon.js';
import {ComponentPropsWithRef} from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-icon': WebComponentProps<IconElement>;
    }
  }
}

/** Props for the Icon component. */
export interface IconProps extends ComponentPropsWithRef<'md-gb-icon'> {
  fill?: boolean;
  opsz?: number;
  wght?: number;
  grad?: number;
}

/** Material 3 Icon component. */
export function Icon({
  fill,
  opsz,
  wght,
  grad,
  children,
  style = {},
  ...props
}: IconProps) {
  const styles = {
    '--md-icon-fill': fill ? 1 : undefined,
    '--md-icon-opsz': opsz,
    '--md-icon-wght': wght,
    '--md-icon-grad': grad,
    ...style,
  };
  return (
    <md-gb-icon style={styles} {...props}>
      {children}
    </md-gb-icon>
  );
}
