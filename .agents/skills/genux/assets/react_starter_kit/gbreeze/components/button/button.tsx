/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {ButtonElement} from '@material/web/labs/gb/components/button/button-element.js';
import '@material/web/labs/gb/components/button/md-gb-button.js';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-button': WebComponentProps<ButtonElement>;
    }
  }
}

const ButtonWebComponent = createComponent({
  tagName: 'md-gb-button',
  elementClass: ButtonElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
  },
});

/** Props for the Button component. */
export interface ButtonProps
  extends React.ComponentProps<typeof ButtonWebComponent> {
  label: React.ReactNode;
  icon?: React.ReactNode;
  toggle?: boolean;
}

/** Material 3 Button component. */
export function Button({label, icon, toggle, children, ...props}: ButtonProps) {
  if (toggle) {
    props.type = 'toggle';
  }

  return (
    <ButtonWebComponent {...props}>
      {children}
      {icon && <Icon>{icon}</Icon>}
      {label}
    </ButtonWebComponent>
  );
}
