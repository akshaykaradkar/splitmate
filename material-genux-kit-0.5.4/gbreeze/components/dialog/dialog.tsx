/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { ReactNode, useId, type CSSProperties } from 'react';
import { createComponent, WebComponentProps } from '@lit/react';
import { MdDialog as DialogElement } from '@material/web/dialog/dialog';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-dialog': WebComponentProps<DialogElement>;
    }
  }
}

const DialogWebComponent = createComponent({
  tagName: 'md-dialog',
  elementClass: DialogElement,
  react: React,
  events: {
    onclose: 'close',
    onclosed: 'closed',
    onopen: 'open',
    onopened: 'opened',
    oncancel: 'cancel',
  },
});

export interface DialogProps extends Omit<
  React.ComponentProps<typeof DialogWebComponent>,
  'content'
> {
  headline?: ReactNode;
  content?: ReactNode;
  actions?: (formId: string) => ReactNode;
}

export function Dialog({
  headline,
  content,
  actions,
  children,
  style = {},
  ...props
}: DialogProps) {
  // Fix padding/margin resets from Tailwind.
  style = {
    margin: 'auto',
    ...style,
  };
  const headlineStyle: CSSProperties = {
    padding: '24px 24px 0',
  };
  const contentStyle: CSSProperties = {
    padding: '24px',
    paddingBottom: actions ? '8px' : undefined,
  };
  const actionsStyle: CSSProperties = {
    padding: '16px 24px 24px',
  };

  const formId = useId();
  return (
    <DialogWebComponent style={style} {...props}>
      {headline && (
        <div slot="headline" style={headlineStyle}>
          {headline}
        </div>
      )}
      {content && (
        <form slot="content" id={formId} method="dialog" style={contentStyle}>
          {content}
        </form>
      )}
      {actions && (
        <div slot="actions" style={actionsStyle}>
          {actions(formId)}
        </div>
      )}
      {children}
    </DialogWebComponent>
  );
}
