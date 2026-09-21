/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { MdFilledTextField as FilledTextFieldElement } from '@material/web/textfield/filled-text-field';
import { MdOutlinedTextField as OutlinedTextFieldElement } from '@material/web/textfield/outlined-text-field';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-filled-text-field': WebComponentProps<FilledTextFieldElement>;
      'md-outlined-text-field': WebComponentProps<OutlinedTextFieldElement>;
    }
  }
}

const FilledTextFieldWebComponent = createComponent({
  tagName: 'md-filled-text-field',
  elementClass: FilledTextFieldElement,
  react: React,
  events: {
    onchange: 'change',
    oninput: 'input',
  },
});

const OutlinedTextFieldWebComponent = createComponent({
  tagName: 'md-outlined-text-field',
  elementClass: OutlinedTextFieldElement,
  react: React,
  events: {
    onchange: 'change',
    oninput: 'input',
  },
});

interface FilledTextFieldProps extends React.ComponentProps<
  typeof FilledTextFieldWebComponent
> {
  variant?: 'filled';
}
interface OutlinedTextFieldProps extends React.ComponentProps<
  typeof OutlinedTextFieldWebComponent
> {
  variant: 'outlined';
}
export type TextFieldProps = FilledTextFieldProps | OutlinedTextFieldProps;

export function TextField({ variant, ...props }: TextFieldProps) {
  if (variant === 'outlined') {
    return (
      <OutlinedTextFieldWebComponent {...(props as OutlinedTextFieldProps)} />
    );
  }
  return <FilledTextFieldWebComponent {...(props as FilledTextFieldProps)} />;
}