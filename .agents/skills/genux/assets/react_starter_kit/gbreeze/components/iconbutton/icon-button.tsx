/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {IconButtonElement} from '@material/web/labs/gb/components/iconbutton/icon-button-element.js';
import '@material/web/labs/gb/components/iconbutton/md-gb-icon-button.js';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-icon-button': WebComponentProps<IconButtonElement>;
    }
  }
}

const IconButtonWebComponent = createComponent({
  tagName: 'md-gb-icon-button',
  elementClass: IconButtonElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
  },
});

/** Props for the IconButton component. */
export interface IconButtonProps
  extends React.ComponentProps<typeof IconButtonWebComponent> {
  icon: React.ReactNode;
  toggle?: boolean;
}

/** Material 3 IconButton component. */
export function IconButton({icon, toggle, ...props}: IconButtonProps) {
  if (toggle) {
    props.type = 'toggle';
  }

  return (
    <IconButtonWebComponent {...props}>
      <Icon>{icon}</Icon>
    </IconButtonWebComponent>
  );
}
