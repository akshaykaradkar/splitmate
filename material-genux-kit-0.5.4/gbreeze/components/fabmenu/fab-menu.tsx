/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {
  ComponentPropsWithRef,
  useId,
  useRef,
  useState,
} from 'react';
import './fab-menu.css';
import { Icon } from '../../styles/icons/icon';

export interface FabAction {
  icon: string;
  label: string;
  onClick: () => void;
}

export interface FabMenuProps extends ComponentPropsWithRef<'div'> {
  icon?: string;
  activeIcon?: string;
  actions: FabAction[];
  variant?: 'primary' | 'secondary' | 'tertiary';
  size?: 'md' | 'lg';
}

export function FabMenu({
  icon = 'add',
  activeIcon = 'close',
  actions,
  variant = 'primary',
  size,
  ...props
}: FabMenuProps) {
  const popoverRef = useRef<HTMLDivElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const popoverId = useId();
  const classes = [
    'fab',
    `fab-${variant}-container`,
    size && `fab-${size}`,
    'ripple',
    'focus-ring-outer',
  ];
  return (
    <>
      <button className={classes.join(' ')} popoverTarget={popoverId}>
        <Icon className="fab-icon">{isOpen ? activeIcon : icon}</Icon>
      </button>
      <div
        {...props}
        id={popoverId}
        popover="auto"
        ref={popoverRef}
        onToggle={(e: ToggleEvent) => {
          setIsOpen(e.newState === 'open');
        }}
        className="fab-menu"
      >
        {actions.map((action, index) => (
          <div key={index} className="fab-menu-action">
            <span className="fab-menu-action-label">{action.label}</span>
            <button
              onClick={() => {
                action.onClick();
                popoverRef.current?.hidePopover();
              }}
              className="fab-menu-action-button ripple"
            >
              <Icon>{action.icon}</Icon>
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
