/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Switch as SwitchElement } from '@material/web/labs/gb/components/switch/md-switch';
import { Icon } from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-switch': WebComponentProps<SwitchElement>;
    }
  }
}

const SwitchWebComponent = createComponent({
  tagName: 'md-switch',
  elementClass: SwitchElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
    oninput: 'input',
  },
});

export interface SwitchProps extends React.ComponentProps<
  typeof SwitchWebComponent
> {
  onIcon?: React.ReactNode;
  offIcon?: React.ReactNode;
}

export function Switch({ onIcon, offIcon, ...props }: SwitchProps) {
  return (
    <SwitchWebComponent {...props}>
      {onIcon && <Icon slot="on-icon">{onIcon}</Icon>}
      {offIcon && <Icon slot="off-icon">{offIcon}</Icon>}
    </SwitchWebComponent>
  );
}
