/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { SplitButton as SplitButtonElement } from '@material/web/labs/gb/components/splitbutton/md-split-button';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-split-button': WebComponentProps<SplitButtonElement>;
    }
  }
}

const SplitButtonWebComponent = createComponent({
  tagName: 'md-split-button',
  elementClass: SplitButtonElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

export interface SplitButtonProps extends Omit<
  React.ComponentProps<typeof SplitButtonWebComponent>,
  'onClick'
> {
  label: React.ReactNode;
  menuId: string;
  onClick?: React.MouseEventHandler<HTMLButtonElement>;
}

export function SplitButton({
  label,
  menuId,
  'aria-label': ariaLabel,
  onClick,
  children,
  ...props
}: SplitButtonProps) {
  return (
    <SplitButtonWebComponent {...props}>
      <button
        slot="leading"
        style={{ all: 'inherit', display: 'flex' }}
        onClick={onClick}
        aria-label={ariaLabel}
      >
        {label}
      </button>
      <button slot="trailing" popoverTarget={menuId}></button>
      {children}
    </SplitButtonWebComponent>
  );
}
