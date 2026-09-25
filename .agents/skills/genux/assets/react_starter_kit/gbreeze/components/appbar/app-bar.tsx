/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {AppBarElement} from '@material/web/labs/gb/components/appbar/app-bar-element.js';
import '@material/web/labs/gb/components/appbar/md-gb-app-bar.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-app-bar': WebComponentProps<AppBarElement>;
    }
  }
}

const AppBarWebComponent = createComponent({
  tagName: 'md-gb-app-bar',
  elementClass: AppBarElement,
  react: React,
  events: {
    onscrollstatechange: 'scrollstatechange',
  },
});

/** Size variants for the AppBar component. */
export type AppBarSize = 'sm' | 'md' | 'lg';
/** Layout variants for the AppBar component. */
export type AppBarVariant =
  | 'standard'
  | 'search'
  | 'small'
  | 'medium'
  | 'large'
  | 'center-aligned';

/** Props for the AppBar component. */
export interface AppBarProps
  extends Omit<
    React.ComponentProps<typeof AppBarWebComponent>,
    'title' | 'size' | 'variant'
  > {
  size?: AppBarSize;
  variant?: AppBarVariant;
  scrolled?: boolean;
  isScrolled?: boolean;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  leading?: React.ReactNode;
  trailing?: React.ReactNode;
  search?: React.ReactNode;
  children?: React.ReactNode;
}

interface SlotElementProps {
  children?: React.ReactNode;
  slot?: string;
}

function renderSlot(
  content: React.ReactNode,
  slotName: string,
): React.ReactNode {
  if (content === undefined || content === null || content === false) {
    return null;
  }
  return React.Children.map(content, (child) => {
    if (!React.isValidElement<SlotElementProps>(child)) {
      return <span slot={slotName}>{child}</span>;
    }
    if (child.type === React.Fragment) {
      return renderSlot(child.props.children, slotName);
    }
    return React.cloneElement(child, {
      slot: child.props.slot ?? slotName,
    });
  });
}

/** Material 3 AppBar component for top navigation and actions. */
export function AppBar({
  size,
  variant = 'standard',
  scrolled,
  isScrolled,
  title,
  subtitle,
  leading,
  trailing,
  search,
  children,
  ...props
}: AppBarProps) {
  let resolvedSize: AppBarSize = size ?? 'sm';
  let resolvedVariant: 'standard' | 'search' = 'standard';

  if (variant === 'search') {
    resolvedVariant = 'search';
  } else if (variant === 'medium') {
    resolvedSize = size ?? 'md';
  } else if (variant === 'large') {
    resolvedSize = size ?? 'lg';
  } else if (variant === 'small' || variant === 'center-aligned') {
    resolvedSize = size ?? 'sm';
  }

  const effectiveScrolled = scrolled ?? isScrolled ?? false;

  return (
    <AppBarWebComponent
      size={resolvedSize}
      variant={resolvedVariant}
      scrolled={effectiveScrolled}
      {...props}>
      {renderSlot(leading, 'leading')}
      {title}
      {children}
      {renderSlot(subtitle, 'subtitle')}
      {renderSlot(trailing, 'trailing')}
      {renderSlot(search, 'search')}
    </AppBarWebComponent>
  );
}

/** Alias props type for TopAppBar. */
export type TopAppBarProps = AppBarProps;
/** Alias component for AppBar. */
export const TopAppBar = AppBar;
