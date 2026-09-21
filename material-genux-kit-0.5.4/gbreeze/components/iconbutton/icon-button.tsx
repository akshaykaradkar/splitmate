/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Icon } from '../../styles/icons/icon';
import { IconButton as IconButtonElement } from '@material/web/labs/gb/components/iconbutton/md-icon-button';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-icon-button': WebComponentProps<IconButtonElement>;
    }
  }
}

const IconButtonWebComponent = createComponent({
  tagName: 'md-icon-button',
  elementClass: IconButtonElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
  },
});

export interface IconButtonProps extends React.ComponentProps<
  typeof IconButtonWebComponent
> {
  icon: React.ReactNode;
  toggle?: boolean;
}

export function IconButton({ icon, toggle, ...props }: IconButtonProps) {
  if (toggle) {
    props.type = 'toggle';
  }

  return (
    <IconButtonWebComponent {...props}>
      <Icon>{icon}</Icon>
    </IconButtonWebComponent>
  );
}
