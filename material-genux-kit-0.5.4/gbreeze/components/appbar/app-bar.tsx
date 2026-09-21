/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef} from 'react';
import './app-bar.css';
import { Icon } from '../../styles/icons/icon';

export interface TopAppBarProps extends ComponentPropsWithRef<'header'> {
  variant?: 'center-aligned' | 'small' | 'medium' | 'large';
  title: string;
  navigationIcon?: string;
  actionIcons?: string[];
  isScrolled?: boolean;
}

export function TopAppBar({
  variant = 'small',
  title,
  navigationIcon,
  actionIcons = [],
  className = '',
  isScrolled = false,
  ...props
}: TopAppBarProps) {
  const renderNavIcon = () =>
    navigationIcon && (
      <button className="icon-btn icon-btn-sm icon-btn-standard ripple focus-ring-outer nav-icon">
        <Icon>{navigationIcon}</Icon>
      </button>
    );

  const renderActions = () =>
    actionIcons.length > 0 && (
      <div className="actions">
        {actionIcons.map((icon, index) => (
          <button
            key={index}
            className="icon-btn icon-btn-sm icon-btn-standard ripple focus-ring-outer"
          >
            <Icon>{icon}</Icon>
          </button>
        ))}
      </div>
    );

  const isExpanded = variant === 'medium' || variant === 'large';
  const headerClasses = [
    !isExpanded && 'top-app-bar',
    variant === 'center-aligned' && 'center-aligned-top-app-bar',
    variant === 'small' && 'small-top-app-bar',
    variant === 'medium' && 'medium-top-app-bar',
    variant === 'large' && 'large-top-app-bar',
    isScrolled && 'top-app-bar-scrolled',
    className,
  ];

  return (
    <header className={headerClasses.filter(Boolean).join(' ')} {...props}>
      {isExpanded ? (
        <>
          <div className="top-content">
            {renderNavIcon()}
            {renderActions()}
          </div>
          <div className="headline-container">
            <h1 className="headline">{title}</h1>
          </div>
        </>
      ) : (
        <>
          {renderNavIcon()}
          <h1 className="headline">{title}</h1>
          {renderActions()}
        </>
      )}
    </header>
  );
}
