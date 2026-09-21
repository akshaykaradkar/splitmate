/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { Icon } from '../../styles/icons/icon';
import { Button as ButtonElement } from '@material/web/labs/gb/components/button/md-button';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-button': WebComponentProps<ButtonElement>;
    }
  }
}

const ButtonWebComponent = createComponent({
  tagName: 'md-button',
  elementClass: ButtonElement,
  react: React,
  events: {
    onclick: 'click',
    onchange: 'change',
  },
});

export interface ButtonProps extends React.ComponentProps<
  typeof ButtonWebComponent
> {
  label: React.ReactNode;
  icon?: React.ReactNode;
  toggle?: boolean;
}

export function Button({
  label,
  icon,
  toggle,
  children,
  ...props
}: ButtonProps) {
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
