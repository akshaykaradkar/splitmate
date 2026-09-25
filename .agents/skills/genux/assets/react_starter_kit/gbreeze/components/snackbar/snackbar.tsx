/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  ComponentPropsWithRef,
  forwardRef,
  ReactNode,
  useEffect,
  useRef,
  useState,
} from 'react';
import {Icon} from '../../styles/icons/icon';
import {useRipple} from '../ripple/ripple';
import './snackbar.css';

/** Props for the Snackbar component. */
export interface SnackbarProps
  extends Omit<ComponentPropsWithRef<'div'>, 'children'> {
  message?: ReactNode;
  children?: ReactNode;
  action?: ReactNode;
  actionLabel?: ReactNode;
  onAction?: () => void;
  onClose?: () => void;
  onDismiss?: () => void;
  closeable?: boolean;
  showClose?: boolean;
  showCloseIcon?: boolean;
  duration?: number;
  timeoutMs?: number;
  isOpen?: boolean;
  open?: boolean;
  multiline?: boolean;
  twoLine?: boolean;
}

interface SnackbarActionButtonProps {
  onAction?: () => void;
  children: ReactNode;
}

function SnackbarActionButton({onAction, children}: SnackbarActionButtonProps) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  useRipple(buttonRef);

  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={onAction}
      className="btn btn-text btn-sm snackbar-action-button ripple focus-ring-outer">
      {children}
    </button>
  );
}

interface SnackbarCloseButtonProps {
  onClose?: () => void;
}

function SnackbarCloseButton({onClose}: SnackbarCloseButtonProps) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  useRipple(buttonRef);

  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={onClose}
      aria-label="Close"
      className="icon-btn icon-btn-standard icon-btn-sm snackbar-close-button ripple focus-ring-outer">
      <Icon>close</Icon>
    </button>
  );
}

/** Material 3 Snackbar component. */
export const Snackbar = forwardRef<HTMLDivElement, SnackbarProps>(
  function Snackbar(
    {
      message,
      children,
      action,
      actionLabel,
      onAction,
      onClose,
      onDismiss,
      closeable,
      showClose,
      showCloseIcon,
      duration,
      timeoutMs,
      isOpen,
      open,
      multiline,
      twoLine,
      className = '',
      onMouseEnter,
      onMouseLeave,
      onFocus,
      onBlur,
      onKeyDown,
      ...props
    },
    ref,
  ) {
    const content = message ?? children;
    const resolvedAction = action ?? actionLabel;
    const handleClose = onClose ?? onDismiss;
    const isCloseable = closeable ?? showClose ?? showCloseIcon ?? false;
    const isVisible = isOpen ?? open ?? true;
    const isTwoLine = multiline ?? twoLine ?? false;

    // Actionable snackbars default to persistent (0ms); non-actionable default to 4000ms.
    const isActionable = Boolean(resolvedAction);
    const resolvedTimeout =
      timeoutMs !== undefined
        ? timeoutMs
        : duration !== undefined
          ? duration
          : isActionable
            ? 0
            : 4000;

    const [isPaused, setIsPaused] = useState(false);
    const isHoveredRef = useRef(false);
    const isFocusedRef = useRef(false);
    const remainingRef = useRef<number>(resolvedTimeout);
    const startTimeRef = useRef<number | null>(null);
    const prevVisibleRef = useRef(isVisible);

    const handleCloseRef = useRef(handleClose);
    handleCloseRef.current = handleClose;

    // Reset timer only when transitioning from invisible to visible
    useEffect(() => {
      if (isVisible && !prevVisibleRef.current) {
        remainingRef.current = resolvedTimeout;
        setIsPaused(false);
      }
      prevVisibleRef.current = isVisible;
    }, [isVisible, resolvedTimeout]);

    useEffect(() => {
      if (!isVisible || resolvedTimeout <= 0 || isPaused) {
        return;
      }

      if (remainingRef.current <= 0) {
        handleCloseRef.current?.();
        return;
      }

      startTimeRef.current = Date.now();
      const timer = setTimeout(() => {
        handleCloseRef.current?.();
      }, remainingRef.current);

      return () => {
        clearTimeout(timer);
        if (startTimeRef.current !== null) {
          const elapsed = Date.now() - startTimeRef.current;
          remainingRef.current = Math.max(0, remainingRef.current - elapsed);
          startTimeRef.current = null;
        }
      };
    }, [isVisible, resolvedTimeout, isPaused]);

    if (!isVisible) {
      return null;
    }

    const hasActions = Boolean(resolvedAction || isCloseable);
    const twoLineClass = isTwoLine ? 'snackbar-two-line' : '';
    const actionsClass = hasActions ? 'has-actions' : '';

    const handleMouseEnter = (e: React.MouseEvent<HTMLDivElement>) => {
      isHoveredRef.current = true;
      setIsPaused(true);
      onMouseEnter?.(e);
    };

    const handleMouseLeave = (e: React.MouseEvent<HTMLDivElement>) => {
      isHoveredRef.current = false;
      setIsPaused(isHoveredRef.current || isFocusedRef.current);
      onMouseLeave?.(e);
    };

    const handleFocus = (e: React.FocusEvent<HTMLDivElement>) => {
      isFocusedRef.current = true;
      setIsPaused(true);
      onFocus?.(e);
    };

    const handleBlur = (e: React.FocusEvent<HTMLDivElement>) => {
      if (!e.currentTarget.contains(e.relatedTarget as Node)) {
        isFocusedRef.current = false;
        setIsPaused(isHoveredRef.current || isFocusedRef.current);
      }
      onBlur?.(e);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
      if (e.key === 'Escape') {
        handleCloseRef.current?.();
      }
      onKeyDown?.(e);
    };

    return (
      <div
        ref={ref}
        role="status"
        aria-live="polite"
        className={`snackbar ${twoLineClass} ${actionsClass} ${className}`.trim()}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        onFocus={handleFocus}
        onBlur={handleBlur}
        onKeyDown={handleKeyDown}
        {...props}>
        <div className="snackbar-message">{content}</div>
        {hasActions && (
          <div className="snackbar-actions">
            {resolvedAction && (
              <SnackbarActionButton onAction={onAction}>
                {resolvedAction}
              </SnackbarActionButton>
            )}
            {isCloseable && (
              <SnackbarCloseButton onClose={() => handleCloseRef.current?.()} />
            )}
          </div>
        )}
      </div>
    );
  },
);
