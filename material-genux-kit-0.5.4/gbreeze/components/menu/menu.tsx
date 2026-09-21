/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { createComponent, type WebComponentProps } from '@lit/react';
import { Menu as MenuElement } from '@material/web/labs/gb/components/menu/md-menu';
import { MenuItem as MenuItemElement } from '@material/web/labs/gb/components/menu/md-menu-item';
import { MenuGroup as MenuGroupElement } from '@material/web/labs/gb/components/menu/md-menu-group';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-menu': WebComponentProps<MenuElement>;
      'md-menu-item': WebComponentProps<MenuItemElement>;
      'md-menu-group': WebComponentProps<MenuGroupElement>;
    }
  }
}

const MenuWebComponent = createComponent({
  tagName: 'md-menu',
  elementClass: MenuElement,
  react: React,
  events: {},
});

const MenuItemWebComponent = createComponent({
  tagName: 'md-menu-item',
  elementClass: MenuItemElement,
  react: React,
  events: {
    onclick: 'click',
  },
});

const MenuGroupWebComponent = createComponent({
  tagName: 'md-menu-group',
  elementClass: MenuGroupElement,
  react: React,
  events: {},
});

export interface MenuProps extends React.ComponentProps<
  typeof MenuWebComponent
> {
  children: React.ReactNode;
}

export function Menu({ children, ...props }: MenuProps) {
  return <MenuWebComponent {...props}>{children}</MenuWebComponent>;
}

export interface MenuItemProps extends React.ComponentProps<
  typeof MenuItemWebComponent
> {
  label?: React.ReactNode;
  leadingContent?: React.ReactNode;
  supportingText?: React.ReactNode;
  trailingText?: React.ReactNode;
  trailingContent?: React.ReactNode;
}

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


export interface MenuGroupProps extends React.ComponentProps<
  typeof MenuGroupWebComponent
> {
  children: React.ReactNode;
}

export function MenuGroup({ children, ...props }: MenuGroupProps) {
  return <MenuGroupWebComponent {...props}>{children}</MenuGroupWebComponent>;
}


