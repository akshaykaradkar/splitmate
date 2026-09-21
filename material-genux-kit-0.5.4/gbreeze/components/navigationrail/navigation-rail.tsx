/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, ReactNode} from 'react';
import './navigation-rail.css';
import { Icon } from '../../styles/icons/icon';

export interface NavigationRailProps extends ComponentPropsWithRef<'nav'> {
  children: ReactNode;
  header?: ReactNode;
  fab?: ReactNode;
}

export function NavigationRail({
  children,
  header,
  fab,
  className,
  ...props
}: NavigationRailProps) {
  return (
    <nav className={`navigation-rail ${className ?? ''}`} {...props}>
      {header && <div className="navigation-rail-menu-button">{header}</div>}
      {fab && <div className="navigation-rail-fab">{fab}</div>}
      <div className="navigation-rail-items">{children}</div>
    </nav>
  );
}

export interface NavigationRailItemProps
  extends ComponentPropsWithRef<'button'> {
  icon: string;
  activeIcon?: string;
  label: string;
  active?: boolean;
}

export function NavigationRailItem({
  icon,
  activeIcon,
  label,
  active = false,
  className,
  ...props
}: NavigationRailItemProps) {
  return (
    <button
      className={`navigation-rail-item ripple-host focus-ring-outer ${active ? 'active' : ''} ${className ?? ''}`}
      aria-selected={active}
      role="tab"
      {...props}
    >
      <div className="navigation-rail-item-icon-container ripple">
        <Icon>{active && activeIcon ? activeIcon : icon}</Icon>
      </div>
      <span className="navigation-rail-item-label">{label}</span>
    </button>
  );
}
