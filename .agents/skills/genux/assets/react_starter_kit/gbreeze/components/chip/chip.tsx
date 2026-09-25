/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {MdAssistChip} from '@material/web/chips/assist-chip';
import {MdChipSet} from '@material/web/chips/chip-set';
import {MdFilterChip} from '@material/web/chips/filter-chip';
import {MdInputChip} from '@material/web/chips/input-chip';
import {MdSuggestionChip} from '@material/web/chips/suggestion-chip';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-chip-set': WebComponentProps<MdChipSet>;
      'md-assist-chip': WebComponentProps<MdAssistChip>;
      'md-filter-chip': WebComponentProps<MdFilterChip>;
      'md-input-chip': WebComponentProps<MdInputChip>;
      'md-suggestion-chip': WebComponentProps<MdSuggestionChip>;
    }
  }
}

const ChipSetWebComponent = createComponent({
  tagName: 'md-chip-set',
  elementClass: MdChipSet,
  react: React,
});

/** Props for the ChipSet component. */
export interface ChipSetProps
  extends React.ComponentProps<typeof ChipSetWebComponent> {}

/** Material 3 ChipSet component. */
export function ChipSet(props: ChipSetProps) {
  return <ChipSetWebComponent {...props} />;
}

const AssistChipWebComponent = createComponent({
  tagName: 'md-assist-chip',
  elementClass: MdAssistChip,
  react: React,
  events: {
    onclick: 'click',
  },
});

const FilterChipWebComponent = createComponent({
  tagName: 'md-filter-chip',
  elementClass: MdFilterChip,
  react: React,
  events: {
    onclick: 'click',
    onremove: 'remove',
  },
});

const InputChipWebComponent = createComponent({
  tagName: 'md-input-chip',
  elementClass: MdInputChip,
  react: React,
  events: {
    onclick: 'click',
    onremove: 'remove',
  },
});

const SuggestionChipWebComponent = createComponent({
  tagName: 'md-suggestion-chip',
  elementClass: MdSuggestionChip,
  react: React,
  events: {
    onclick: 'click',
  },
});

function renderIcon(icon?: string, avatar?: boolean) {
  if (icon) {
    return (
      <Icon
        slot="icon"
        // Tailwind resets margin
        style={
          {
            marginInlineEnd: 'var(--_icon-label-space)',
            '--md-icon-size': avatar
              ? 'var(--_avatar-size)'
              : 'var(--_icon-size)',
          } as React.CSSProperties
        }>
        {icon}
      </Icon>
    );
  }
  return null;
}

/** Props for the AssistChip component. */
export interface AssistChipProps
  extends Omit<React.ComponentProps<typeof AssistChipWebComponent>, 'label'> {
  label?: React.ReactNode;
  icon?: string;
}

/** Material 3 AssistChip component. */
export function AssistChip({label, icon, ...props}: AssistChipProps) {
  return (
    <AssistChipWebComponent {...props}>
      {renderIcon(icon)}
      {label}
    </AssistChipWebComponent>
  );
}

/** Props for the FilterChip component. */
export interface FilterChipProps
  extends Omit<React.ComponentProps<typeof FilterChipWebComponent>, 'label'> {
  label?: React.ReactNode;
  icon?: string;
}

/** Material 3 FilterChip component. */
export function FilterChip({label, icon, ...props}: FilterChipProps) {
  return (
    <FilterChipWebComponent {...props}>
      {renderIcon(icon)}
      {label}
    </FilterChipWebComponent>
  );
}

/** Props for the InputChip component. */
export interface InputChipProps
  extends Omit<React.ComponentProps<typeof InputChipWebComponent>, 'label'> {
  label?: React.ReactNode;
  icon?: string;
}

/** Material 3 InputChip component. */
export function InputChip({label, icon, ...props}: InputChipProps) {
  return (
    <InputChipWebComponent {...props}>
      {renderIcon(icon, props.avatar)}
      {label}
    </InputChipWebComponent>
  );
}

/** Props for the SuggestionChip component. */
export interface SuggestionChipProps
  extends Omit<
    React.ComponentProps<typeof SuggestionChipWebComponent>,
    'label'
  > {
  label?: React.ReactNode;
  icon?: string;
}

/** Material 3 SuggestionChip component. */
export function SuggestionChip({label, icon, ...props}: SuggestionChipProps) {
  return (
    <SuggestionChipWebComponent {...props}>
      {renderIcon(icon)}
      {label}
    </SuggestionChipWebComponent>
  );
}
