/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import {ChipElement} from '@material/web/labs/gb/components/chip/chip-element.js';
import '@material/web/labs/gb/components/chip/md-gb-chip.js';
import React from 'react';
import {Icon} from '../../styles/icons/icon';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-chip': WebComponentProps<ChipElement>;
    }
  }
}

const ChipWebComponent = createComponent({
  tagName: 'md-gb-chip',
  elementClass: ChipElement,
  react: React,
  events: {
    onclick: 'click',
    onClick: 'click',
    onremove: 'remove',
    onRemove: 'remove',
    onchange: 'change',
    onChange: 'change',
    oninput: 'input',
    onInput: 'input',
  },
});

/** Color style variants for ExpressiveChip. */
export type ExpressiveChipColor = 'elevated' | 'filled' | 'outlined' | 'tonal';
/** Behavioral type variants for ExpressiveChip. */
export type ExpressiveChipType = 'action' | 'filter' | 'toggle' | 'link';

/** Props for the ExpressiveChip component. */
export interface ExpressiveChipProps
  extends Omit<
    React.ComponentProps<typeof ChipWebComponent>,
    'label' | 'soft-disabled' | 'color' | 'type'
  > {
  color?: ExpressiveChipColor;
  type?: ExpressiveChipType;
  label?: React.ReactNode;
  icon?: React.ReactNode;
  avatar?: React.ReactNode;
  removeIcon?: React.ReactNode;
  softDisabled?: boolean;
  'soft-disabled'?: boolean;
  children?: React.ReactNode;
}

interface SlotElementProps {
  children?: React.ReactNode;
  slot?: string;
  className?: string;
}

function renderSlot(
  content: React.ReactNode,
  slotName: string,
  extraClassName?: string,
): React.ReactNode {
  if (content == null || typeof content === 'boolean') {
    return null;
  }
  if (typeof content === 'string') {
    if (extraClassName) {
      return (
        <span slot={slotName} className={extraClassName}>
          {content}
        </span>
      );
    }
    return <Icon slot={slotName}>{content}</Icon>;
  }
  return React.Children.map(content, (child) => {
    if (!React.isValidElement<SlotElementProps>(child)) {
      return (
        <span slot={slotName} className={extraClassName}>
          {child}
        </span>
      );
    }
    if (child.type === React.Fragment) {
      return renderSlot(child.props.children, slotName, extraClassName);
    }
    const existingClass = child.props.className ?? '';
    const newClass = extraClassName
      ? `${existingClass} ${extraClassName}`.trim()
      : existingClass || undefined;
    return React.cloneElement(child, {
      slot: child.props.slot ?? slotName,
      className: newClass,
    });
  });
}

/** Material 3 ExpressiveChip component. */
export function ExpressiveChip({
  color = 'outlined',
  type = 'action',
  label,
  icon,
  avatar,
  removeIcon,
  softDisabled,
  'soft-disabled': softDisabledProp,
  children,
  ...props
}: ExpressiveChipProps) {
  const isSoftDisabled = softDisabled ?? softDisabledProp;

  return (
    <ChipWebComponent
      color={color}
      type={type}
      softDisabled={isSoftDisabled}
      {...props}>
      {avatar && renderSlot(avatar, 'icon', 'chip-avatar')}
      {!avatar && renderSlot(icon, 'icon')}
      {label}
      {children}
      {renderSlot(removeIcon, 'remove-icon')}
    </ChipWebComponent>
  );
}
