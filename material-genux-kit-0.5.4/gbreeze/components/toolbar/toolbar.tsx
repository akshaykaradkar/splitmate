/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, ReactNode} from 'react';
import './toolbar.css';
import { Icon } from '../../styles/icons/icon';

export interface ToolbarProps extends ComponentPropsWithRef<'div'> {
  children: ReactNode;
}

export function Toolbar({children, className = '', ...props}: ToolbarProps) {
  return (
    <div className={`toolbar ${className ?? ''}`} {...props}>
      {children}
    </div>
  );
}

export function ToolbarButton({
  icon,
  active,
  onClick,
}: {
  icon: ReactNode;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`toolbar-button ripple ${
        active ? 'toolbar-button-active' : ''
      }`}
    >
      <Icon>{icon}</Icon>
    </button>
  );
}

export function ToolbarSeparator() {
  return <div className="toolbar-separator" />;
}
