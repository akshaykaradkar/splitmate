/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Checkbox as CheckboxElement } from '@material/web/labs/gb/components/checkbox/md-checkbox';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-checkbox': WebComponentProps<CheckboxElement>;
    }
  }
}

const CheckboxWebComponent = createComponent({
  tagName: 'md-checkbox',
  elementClass: CheckboxElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
    oninput: 'input',
  },
});

export interface CheckboxProps extends React.ComponentProps<
  typeof CheckboxWebComponent
> {}

export function Checkbox(props: CheckboxProps) {
  return <CheckboxWebComponent {...props} />;
}
