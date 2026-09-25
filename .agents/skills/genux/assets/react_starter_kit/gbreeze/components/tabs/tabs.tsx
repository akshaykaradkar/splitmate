/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {MdPrimaryTab as PrimaryTabElement} from '@material/web/tabs/primary-tab';
import {MdSecondaryTab as SecondaryTabElement} from '@material/web/tabs/secondary-tab';
import {MdTabs as TabsElement} from '@material/web/tabs/tabs';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

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

/** Props for the Tabs component. */
export interface TabsProps
  extends React.ComponentProps<typeof TabsWebComponent> {}
/** Material 3 Tabs component. */
export function Tabs(props: TabsProps) {
  return <TabsWebComponent {...props} />;
}

/** Props for the PrimaryTab component. */
export interface PrimaryTabProps
  extends React.ComponentProps<typeof PrimaryTabWebComponent> {
  icon?: React.ReactNode;
}
/** Material 3 PrimaryTab component. */
export function PrimaryTab({icon, children, ...props}: PrimaryTabProps) {
  return (
    <PrimaryTabWebComponent {...props}>
      {icon && <Icon slot="icon">{icon}</Icon>}
      {children}
    </PrimaryTabWebComponent>
  );
}

/** Props for the SecondaryTab component. */
export interface SecondaryTabProps
  extends React.ComponentProps<typeof SecondaryTabWebComponent> {}
/** Material 3 SecondaryTab component. */
export function SecondaryTab(props: SecondaryTabProps) {
  return <SecondaryTabWebComponent {...props} />;
}
