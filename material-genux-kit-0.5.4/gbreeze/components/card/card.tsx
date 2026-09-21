/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  ComponentPropsWithRef,
  useEffect,
  useRef,
  useImperativeHandle,
} from 'react';
import { adoptStyles } from '@material/web/labs/gb/styles/adopt-styles.js';
import { cardClasses } from '@material/web/labs/gb/components/card/card.js';
import { useRipple } from '../ripple/ripple';

import focusRingStyles from '@material/web/labs/gb/components/focus/focus-ring.css' with { type: 'css' };
import rippleStyles from '@material/web/labs/gb/components/ripple/ripple.css' with { type: 'css' };
import cardStyles from '@material/web/labs/gb/components/card/card.css' with { type: 'css' };

export interface CardProps extends Omit<ComponentPropsWithRef<'div'>, 'color'> {
  /** The color of the card. */
  color?: 'elevated' | 'filled' | 'outlined';
  /** Whether the card is interactive. */
  interactive?: boolean;
  /** Whether the card is disabled. */
  disabled?: boolean;
}

export function Card({
  color = 'elevated',
  disabled = false,
  interactive = false,
  className = '',
  children,
  'aria-label': ariaLabel,
  ref,
  ...props
}: CardProps) {
  const innerRef = useRef<HTMLDivElement>(null);

  useImperativeHandle(ref, () => innerRef.current as HTMLDivElement);

  const classes = Object.entries(cardClasses({ color, disabled, interactive }))
    .filter(([, value]) => value)
    .map(([key]) => key)
    .join(' ');

  useEffect(() => {
    if (innerRef.current) {
      adoptStyles(innerRef.current, [
        focusRingStyles,
        rippleStyles,
        cardStyles,
      ]);
    }
  }, []);

  return (
    <div
      ref={innerRef}
      className={`${classes} ${className}`.trim()}
      aria-label={!interactive ? ariaLabel : undefined}
      {...props}
    >
      {interactive && <CardButton disabled={disabled} aria-label={ariaLabel} />}
      {children}
    </div>
  );
}

function CardButton({
  disabled,
  'aria-label': ariaLabel,
}: {
  disabled?: boolean;
  'aria-label'?: string;
}) {
  const btnRef = useRef<HTMLButtonElement>(null);
  useRipple(btnRef);

  return (
    <button
      ref={btnRef}
      className="card-btn ripple focus-ring-target"
      disabled={disabled}
      aria-label={ariaLabel}
    />
  );
}
