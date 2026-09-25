/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {createComponent, type WebComponentProps} from '@lit/react';
import {ListElement} from '@material/web/labs/gb/components/list/list-element.js';
import {ListItemElement} from '@material/web/labs/gb/components/list/list-item-element.js';
import '@material/web/labs/gb/components/list/md-gb-list-item.js';
import '@material/web/labs/gb/components/list/md-gb-list.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-list': WebComponentProps<ListElement>;
      'md-gb-list-item': WebComponentProps<ListItemElement>;
    }
  }
}

const ListWebComponent = createComponent({
  tagName: 'md-gb-list',
  elementClass: ListElement,
  react: React,
  events: {},
});

const ListItemWebComponent = createComponent({
  tagName: 'md-gb-list-item',
  elementClass: ListItemElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

/** Props for the List component. */
export interface ListProps
  extends React.ComponentProps<typeof ListWebComponent> {
  children: React.ReactNode;
}

/** Material 3 List component. */
export function List({children, ...props}: ListProps) {
  return <ListWebComponent {...props}>{children}</ListWebComponent>;
}

/** Props for the ListItem component. */
export interface ListItemProps
  extends React.ComponentProps<typeof ListItemWebComponent> {
  label?: React.ReactNode;
  avatar?: React.ReactNode;
  leadingContent?: React.ReactNode;
  supportingText?: React.ReactNode;
  trailingText?: React.ReactNode;
  trailingContent?: React.ReactNode;
}

/** Configuration item for ListItem. */
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
