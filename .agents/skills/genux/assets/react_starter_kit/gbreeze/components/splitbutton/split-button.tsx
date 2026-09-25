/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import '@material/web/labs/gb/components/splitbutton/md-gb-split-button.js';
import {SplitButtonElement} from '@material/web/labs/gb/components/splitbutton/split-button-element.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-split-button': WebComponentProps<SplitButtonElement>;
    }
  }
}

const SplitButtonWebComponent = createComponent({
  tagName: 'md-gb-split-button',
  elementClass: SplitButtonElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

/** Props for the SplitButton component. */
export interface SplitButtonProps
  extends Omit<
    React.ComponentProps<typeof SplitButtonWebComponent>,
    'onClick'
  > {
  label: React.ReactNode;
  menuId: string;
  onClick?: React.MouseEventHandler<HTMLButtonElement>;
}

/** Material 3 SplitButton component. */
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
        style={{all: 'inherit', display: 'flex'}}
        onClick={onClick}
        aria-label={ariaLabel}>
        {label}
      </button>
      <button slot="trailing" popoverTarget={menuId}></button>
      {children}
    </SplitButtonWebComponent>
  );
}
