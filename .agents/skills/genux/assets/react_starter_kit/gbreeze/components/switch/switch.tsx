/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import '@material/web/labs/gb/components/switch/md-gb-switch.js';
import {SwitchElement} from '@material/web/labs/gb/components/switch/switch-element.js';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-switch': WebComponentProps<SwitchElement>;
    }
  }
}

const SwitchWebComponent = createComponent({
  tagName: 'md-gb-switch',
  elementClass: SwitchElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
    oninput: 'input',
  },
});

/** Props for the Switch component. */
export interface SwitchProps
  extends React.ComponentProps<typeof SwitchWebComponent> {
  onIcon?: React.ReactNode;
  offIcon?: React.ReactNode;
}

/** Material 3 Switch component. */
export function Switch({onIcon, offIcon, ...props}: SwitchProps) {
  return (
    <SwitchWebComponent {...props}>
      {onIcon && <Icon slot="on-icon">{onIcon}</Icon>}
      {offIcon && <Icon slot="off-icon">{offIcon}</Icon>}
    </SwitchWebComponent>
  );
}
