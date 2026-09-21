/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { WebComponentProps, createComponent } from '@lit/react';
import { MdTabs as TabsElement } from '@material/web/tabs/tabs';
import { MdPrimaryTab as PrimaryTabElement } from '@material/web/tabs/primary-tab';
import { MdSecondaryTab as SecondaryTabElement } from '@material/web/tabs/secondary-tab';
import { Icon } from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-tabs': WebComponentProps<TabsElement>;
      'md-primary-tab': WebComponentProps<PrimaryTabElement>;
      'md-secondary-tab': WebComponentProps<SecondaryTabElement>;
    }
  }
}

const TabsWebComponent = createComponent({
  tagName: 'md-tabs',
  elementClass: TabsElement,
  react: React,
  events: {
    onchange: 'change',
  },
});

const PrimaryTabWebComponent = createComponent({
  tagName: 'md-primary-tab',
  elementClass: PrimaryTabElement,
  react: React,
});

const SecondaryTabWebComponent = createComponent({
  tagName: 'md-secondary-tab',
  elementClass: SecondaryTabElement,
  react: React,
});

export interface TabsProps extends React.ComponentProps<
  typeof TabsWebComponent
> {}
export function Tabs(props: TabsProps) {
  return <TabsWebComponent {...props} />;
}

export interface PrimaryTabProps extends React.ComponentProps<
  typeof PrimaryTabWebComponent
> {
  icon?: React.ReactNode;
}
export function PrimaryTab({ icon, children, ...props }: PrimaryTabProps) {
  return (
    <PrimaryTabWebComponent {...props}>
      {icon && <Icon slot="icon">{icon}</Icon>}
      {children}
    </PrimaryTabWebComponent>
  );
}

export interface SecondaryTabProps extends React.ComponentProps<
  typeof SecondaryTabWebComponent
> {}
export function SecondaryTab(props: SecondaryTabProps) {
  return <SecondaryTabWebComponent {...props} />;
}
