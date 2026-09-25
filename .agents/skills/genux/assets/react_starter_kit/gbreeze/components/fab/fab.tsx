/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {FabElement} from '@material/web/labs/gb/components/fab/fab-element.js';
import '@material/web/labs/gb/components/fab/md-gb-fab.js';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-fab': WebComponentProps<FabElement>;
    }
  }
}

const FabWebComponent = createComponent({
  tagName: 'md-gb-fab',
  elementClass: FabElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

/** Props for the Fab component. */
export interface FabProps extends React.ComponentProps<typeof FabWebComponent> {
  icon: React.ReactNode;
  label?: string;
}

/** Material 3 Fab component. */
export function Fab({icon, label, children, ...props}: FabProps) {
  return (
    <FabWebComponent {...props}>
      {children}
      {icon && <Icon>{icon}</Icon>}
      {label}
    </FabWebComponent>
  );
}
