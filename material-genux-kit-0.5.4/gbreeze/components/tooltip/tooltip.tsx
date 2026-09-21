/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, ReactNode} from 'react';
import './tooltip.css';

export interface TooltipProps extends ComponentPropsWithRef<'div'> {
  children: ReactNode;
  position?: 'top' | 'bottom' | 'left' | 'right';
}

export function Tooltip({
  children,
  position = 'top',
  className,
  ...props
}: TooltipProps) {
  return (
    <div
      {...props}
      popover="hint"
      role="tooltip"
      className={`tooltip ${position} ${className ?? ''}`}>
      {children}
    </div>
  );
}
