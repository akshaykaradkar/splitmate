/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentPropsWithRef } from 'react';
import type { WebComponentProps } from '@lit/react';
import type { Icon as IconElement } from '@material/web/labs/gb/styles/icon/md-icon';
import '@material/web/labs/gb/styles/icon/md-icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-icon': WebComponentProps<IconElement>;
    }
  }
}

export interface IconProps extends ComponentPropsWithRef<'md-icon'> {
  fill?: boolean;
  opsz?: number;
  wght?: number;
  grad?: number;
}

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
    <md-icon style={styles} {...props}>
      {children}
    </md-icon>
  );
}
