/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { createComponent, type WebComponentProps } from '@lit/react';
import { List as ListElement } from '@material/web/labs/gb/components/list/md-list';
import { ListItem as ListItemElement } from '@material/web/labs/gb/components/list/md-list-item';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-list': WebComponentProps<ListElement>;
      'md-list-item': WebComponentProps<ListItemElement>;
    }
  }
}

const ListWebComponent = createComponent({
  tagName: 'md-list',
  elementClass: ListElement,
  react: React,
  events: {},
});

const ListItemWebComponent = createComponent({
  tagName: 'md-list-item',
  elementClass: ListItemElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

export interface ListProps extends React.ComponentProps<
  typeof ListWebComponent
> {
  children: React.ReactNode;
}

export function List({ children, ...props }: ListProps) {
  return <ListWebComponent {...props}>{children}</ListWebComponent>;
}

export interface ListItemProps extends React.ComponentProps<
  typeof ListItemWebComponent
> {
  label?: React.ReactNode;
  avatar?: React.ReactNode;
  leadingContent?: React.ReactNode;
  supportingText?: React.ReactNode;
  trailingText?: React.ReactNode;
  trailingContent?: React.ReactNode;
}

export function ListItem({
  label,
  avatar,
  leadingContent,
  supportingText,
  trailingText,
  trailingContent,
  children,
  ...props
}: ListItemProps) {
  return (
    <ListItemWebComponent {...props}>
      {avatar && (
        <span slot="avatar" className="flex">
          {avatar}
        </span>
      )}
      {leadingContent && (
        <span slot="leading" className="flex">
          {leadingContent}
        </span>
      )}
      {label || children}
      {supportingText && (
        <span slot="supporting-text" className="flex">
          {supportingText}
        </span>
      )}
      {trailingText && (
        <span slot="trailing-text" className="flex">
          {trailingText}
        </span>
      )}
      {trailingContent && (
        <span slot="trailing" className="flex">
          {trailingContent}
        </span>
      )}
    </ListItemWebComponent>
  );
}


