/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentPropsWithRef, ReactNode } from 'react';
import './badge.css';

export interface BadgeProps extends ComponentPropsWithRef<'span'> {
  children?: ReactNode;
}

export function Badge({
  className,
  children,
  ...props
}: BadgeProps) {
  const badgeClasses = ['badge', className].filter(Boolean).join(' ');

  return (
    <span className={badgeClasses} {...props}>
      {children}
    </span>
  );
}
