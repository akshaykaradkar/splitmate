/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {createComponent, type WebComponentProps} from '@lit/react';
import '@material/web/labs/gb/components/menu/md-gb-menu-group.js';
import '@material/web/labs/gb/components/menu/md-gb-menu-item.js';
import '@material/web/labs/gb/components/menu/md-gb-menu.js';
import {MenuElement} from '@material/web/labs/gb/components/menu/menu-element.js';
import {MenuGroupElement} from '@material/web/labs/gb/components/menu/menu-group-element.js';
import {MenuItemElement} from '@material/web/labs/gb/components/menu/menu-item-element.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-menu': WebComponentProps<MenuElement>;
      'md-gb-menu-item': WebComponentProps<MenuItemElement>;
      'md-gb-menu-group': WebComponentProps<MenuGroupElement>;
    }
  }
}

const MenuWebComponent = createComponent({
  tagName: 'md-gb-menu',
  elementClass: MenuElement,
  react: React,
  events: {},
});

const MenuItemWebComponent = createComponent({
  tagName: 'md-gb-menu-item',
  elementClass: MenuItemElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

const MenuGroupWebComponent = createComponent({
  tagName: 'md-gb-menu-group',
  elementClass: MenuGroupElement,
  react: React,
  events: {},
});

/** Props for the Menu component. */
export interface MenuProps
  extends React.ComponentProps<typeof MenuWebComponent> {
  children: React.ReactNode;
}

/** Material 3 Menu component. */
export function Menu({children, ...props}: MenuProps) {
  return <MenuWebComponent {...props}>{children}</MenuWebComponent>;
}

/** Props for the MenuItem component. */
export interface MenuItemProps
  extends React.ComponentProps<typeof MenuItemWebComponent> {
  label?: React.ReactNode;
  leadingContent?: React.ReactNode;
  supportingText?: React.ReactNode;
  trailingText?: React.ReactNode;
  trailingContent?: React.ReactNode;
}

/** Configuration item for MenuItem. */
export function MenuItem({
  label,
  leadingContent,
  supportingText,
  trailingText,
  trailingContent,
  children,
  ...props
}: MenuItemProps) {
  return (
    <MenuItemWebComponent {...props}>
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
    </MenuItemWebComponent>
  );
}

/** Props for the MenuGroup component. */
export interface MenuGroupProps
  extends React.ComponentProps<typeof MenuGroupWebComponent> {
  children: React.ReactNode;
}

/** Material 3 MenuGroup component. */
export function MenuGroup({children, ...props}: MenuGroupProps) {
  return <MenuGroupWebComponent {...props}>{children}</MenuGroupWebComponent>;
}
