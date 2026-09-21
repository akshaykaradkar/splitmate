/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, useEffect} from 'react';
import './snackbar.css';
import { Icon } from '../../styles/icons/icon';

export interface SnackbarProps extends ComponentPropsWithRef<'div'> {
  message: string;
  action?: string;
  onAction?: () => void;
  onDismiss?: () => void;
  showClose?: boolean;
  duration?: number; // ms, default 4000
  isOpen: boolean;
}

export function Snackbar({
  message,
  action,
  onAction,
  onDismiss,
  showClose = false,
  duration = 4000,
  isOpen,
  className = '',
  ...props
}: SnackbarProps) {
  useEffect(() => {
    if (isOpen && duration > 0) {
      const timer = setTimeout(() => {
        onDismiss?.();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [isOpen, duration, onDismiss]);

  if (!isOpen) return null;

  return (
    <div className={`snackbar ${className ?? ''}`} {...props}>
      <span className="snackbar-message">{message}</span>
      <div className="snackbar-actions">
        {action && (
          <button onClick={onAction} className="snackbar-action-button ripple">
            {action}
          </button>
        )}
        {showClose && (
          <button onClick={onDismiss} className="snackbar-close-button ripple">
            <Icon>close</Icon>
          </button>
        )}
      </div>
    </div>
  );
}
