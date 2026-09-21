/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Icon } from '../../styles/icons/icon';
import { Fab as FabElement } from '@material/web/labs/gb/components/fab/md-fab';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-fab': WebComponentProps<FabElement>;
    }
  }
}

const FabWebComponent = createComponent({
  tagName: 'md-fab',
  elementClass: FabElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

export interface FabProps extends React.ComponentProps<typeof FabWebComponent> {
  icon: React.ReactNode;
  label?: string;
}

export function Fab({ icon, label, children, ...props }: FabProps) {
  return (
    <FabWebComponent {...props}>
      {children}
      {icon && <Icon>{icon}</Icon>}
      {label}
    </FabWebComponent>
  );
}
