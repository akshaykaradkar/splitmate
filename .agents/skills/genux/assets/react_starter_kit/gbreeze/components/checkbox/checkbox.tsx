/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {CheckboxElement} from '@material/web/labs/gb/components/checkbox/checkbox-element.js';
import '@material/web/labs/gb/components/checkbox/md-gb-checkbox.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-checkbox': WebComponentProps<CheckboxElement>;
    }
  }
}

const CheckboxWebComponent = createComponent({
  tagName: 'md-gb-checkbox',
  elementClass: CheckboxElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
    oninput: 'input',
  },
});

/** Props for the Checkbox component. */
export interface CheckboxProps
  extends React.ComponentProps<typeof CheckboxWebComponent> {}

/** Material 3 Checkbox component. */
export function Checkbox(props: CheckboxProps) {
  return <CheckboxWebComponent {...props} />;
}
