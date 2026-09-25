/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import '@material/web/labs/gb/components/radio/md-gb-radio.js';
import {RadioElement} from '@material/web/labs/gb/components/radio/radio-element.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-radio': WebComponentProps<RadioElement>;
    }
  }
}

const RadioWebComponent = createComponent({
  tagName: 'md-gb-radio',
  elementClass: RadioElement,
  react: React,
  events: {
    onchange: 'change',
    oninput: 'input',
  },
});

/** Props for the Radio component. */
export interface RadioProps
  extends React.ComponentProps<typeof RadioWebComponent> {}

/** Material 3 Radio component. */
export function Radio(props: RadioProps) {
  return <RadioWebComponent {...props} />;
}
