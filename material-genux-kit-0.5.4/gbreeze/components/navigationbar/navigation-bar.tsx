/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, ReactNode} from 'react';
import './navigation-bar.css';
import { Icon } from '../../styles/icons/icon';

export interface NavigationBarProps extends ComponentPropsWithRef<'nav'> {
  children: ReactNode;
}

export function NavigationBar({
  children,
  className,
  ref,
  ...props
}: NavigationBarProps) {
  const classes = ['navigation-bar', className];

  return (
    <nav ref={ref} className={classes.filter(Boolean).join(' ')} {...props}>
      {children}
    </nav>
  );
}

export interface NavigationBarItemProps
  extends ComponentPropsWithRef<'button'> {
  icon: string;
  activeIcon?: string;
  label: string;
  active?: boolean;
}

export function NavigationBarItem({
  icon,
  activeIcon,
  label,
  active = false,
  className,
  ref,
  ...props
}: NavigationBarItemProps) {
  const classes = [
    'navigation-bar-item',
    'ripple',
    'focus-ring-outer',
    active ? 'active' : '',
    className,
  ];

  return (
    <button
      ref={ref}
      className={classes.filter(Boolean).join(' ')}
      aria-selected={active}
      role="tab"
      {...props}
    >
      <div className="navigation-bar-item-icon-container">
        <Icon>{active && activeIcon ? activeIcon : icon}</Icon>
      </div>
      <span className="navigation-bar-item-label">{label}</span>
    </button>
  );
}
