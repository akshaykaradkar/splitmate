/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Radio as RadioElement } from '@material/web/labs/gb/components/radio/md-radio';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-radio': WebComponentProps<RadioElement>;
    }
  }
}

const RadioWebComponent = createComponent({
  tagName: 'md-radio',
  elementClass: RadioElement,
  react: React,
  events: {
    onchange: 'change',
    oninput: 'input',
  },
});

export interface RadioProps extends React.ComponentProps<
  typeof RadioWebComponent
> {}

export function Radio(props: RadioProps) {
  return <RadioWebComponent {...props} />;
}
