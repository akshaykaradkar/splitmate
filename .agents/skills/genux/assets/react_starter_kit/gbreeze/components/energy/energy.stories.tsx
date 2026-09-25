/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {Button} from '../button/button';
import {Card} from '../card/card';
import {Energy, EnergyProps} from './energy';

/** EnergyStoryKnobs interface */
export interface EnergyStoryKnobs extends Omit<EnergyProps, 'color'> {
  energize: boolean;
  color?:
    | 'primary'
    | 'secondary'
    | 'tertiary'
    | 'primary-container'
    | 'secondary-container'
    | 'tertiary-container'
    | 'surface'
    | 'surface-container'
    | 'surface-container-lowest'
    | 'surface-container-low'
    | 'surface-container-high'
    | 'surface-container-highest';
  customColor: string;
  GM3: boolean;
  buttonSize?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  buttonSquare: boolean;
  buttonDisabled: boolean;
  buttonSoftDisabled: boolean;
  buttonToggle: boolean;
  cardDisabled: boolean;
  cardInteractive: boolean;
}

const meta = preview.meta({
  title: 'Components/Energy (Aurora)',
  component: Energy,
  parameters: {
    layout: 'centered',
  },
  args: {
    energize: true,
    color: 'primary',
    customColor: '#000000',
    type: 'accents',
    GM3: false,
    state: 'anticipating',
    intensity: 0.5,
    dynamicIntensity: 0,
    buttonSize: 'sm',
    buttonSquare: false,
    buttonDisabled: false,
    buttonSoftDisabled: false,
    buttonToggle: false,
    cardDisabled: false,
    cardInteractive: false,
  },
  argTypes: {
    energize: {
      control: 'boolean',
      description: 'Whether energy animation is active',
    },
    color: {
      control: 'select',
      options: [
        'primary',
        'secondary',
        'tertiary',
        'primary-container',
        'secondary-container',
        'tertiary-container',
        'surface',
        'surface-container',
        'surface-container-lowest',
        'surface-container-low',
        'surface-container-high',
        'surface-container-highest',
      ],
      description: 'Material Design theme color role',
    },
    customColor: {
      control: 'color',
      description: 'Custom color override (used when not #000000)',
    },
    type: {
      control: 'select',
      options: ['accents', 'hue'],
      description: 'Energy color calculation mode',
    },
    GM3: {
      control: 'boolean',
      description: 'Baseline GM3 mode',
    },
    state: {
      control: 'select',
      options: [
        'idle',
        'anticipating',
        'receiving',
        'processing',
        'responding',
      ],
      description: 'Energy animation state',
    },
    intensity: {
      control: {type: 'range', min: 0, max: 1, step: 0.01},
      description: 'Energy intensity level',
    },
    dynamicIntensity: {
      control: {type: 'range', min: 0, max: 1, step: 0.01},
      description: 'Dynamic intensity (active in receiving/responding states)',
    },
    buttonSize: {
      control: 'select',
      options: ['xs', 'sm', 'md', 'lg', 'xl'],
      description: 'Button size in Button stories',
    },
    buttonSquare: {
      control: 'boolean',
      description: 'Square button variant',
    },
    buttonDisabled: {
      control: 'boolean',
      description: 'Disabled button state',
    },
    buttonSoftDisabled: {
      control: 'boolean',
      description: 'Soft-disabled button state',
    },
    buttonToggle: {
      control: 'boolean',
      description: 'Toggle button mode',
    },
    cardDisabled: {
      control: 'boolean',
      description: 'Disabled card state',
    },
    cardInteractive: {
      control: 'boolean',
      description: 'Interactive card mode',
    },
    active: {table: {disable: true}},
    baseline: {table: {disable: true}},
    children: {table: {disable: true}},
    slot: {table: {disable: true}},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  render: (args) => {
    const color =
      args.customColor && args.customColor !== '#000000'
        ? args.customColor
        : `var(--md-sys-color-${args.color ?? 'primary'})`;

    return (
      <div className="flex flex-wrap items-center gap-6 p-6">
        <Energy
          active={args.energize}
          baseline={args.GM3}
          color={color}
          type={args.type}
          state={args.state}
          intensity={args.intensity}
          dynamicIntensity={args.dynamicIntensity}
          className="flex items-center justify-center rounded-2xl m-6 typescale-label-lg font-medium"
          style={{
            width: '100px',
            height: '50px',
            color: 'var(--md-sys-color-on-surface, inherit)',
          }}>
          Energy
        </Energy>

        <Energy
          active={args.energize}
          baseline={args.GM3}
          color={color}
          type={args.type}
          state={args.state}
          intensity={args.intensity}
          dynamicIntensity={args.dynamicIntensity}
          className="flex items-center justify-center rounded-2xl m-6 typescale-label-lg font-medium"
          style={{
            width: '300px',
            height: '200px',
            color: 'var(--md-sys-color-on-surface, inherit)',
          }}>
          Energy
        </Energy>
      </div>
    );
  },
});

/** Buttons story */
export const Buttons = meta.story({
  render: (args) => {
    const customColor =
      args.customColor && args.customColor !== '#000000'
        ? args.customColor
        : undefined;

    const renderButton = (
      btnColor: 'filled' | 'outlined' | 'elevated' | 'tonal' | 'text',
      label: string,
    ) => (
      <Button
        key={btnColor}
        color={btnColor}
        size={args.buttonSize || 'sm'}
        square={args.buttonSquare}
        disabled={args.buttonDisabled}
        softDisabled={args.buttonSoftDisabled}
        toggle={args.buttonToggle}
        label={label}>
        <Energy
          slot="container"
          active={args.energize}
          type={args.type}
          baseline={args.GM3}
          state={args.state}
          intensity={args.intensity}
          dynamicIntensity={args.dynamicIntensity}
          color={customColor}
        />
      </Button>
    );

    return (
      <div className="flex flex-wrap items-start gap-4 p-6 pb-6">
        {renderButton('filled', 'Filled')}
        {renderButton('outlined', 'Outlined')}
        {renderButton('elevated', 'Elevated')}
        {renderButton('tonal', 'Tonal')}
        {renderButton('text', 'Text')}
      </div>
    );
  },
});

/** Cards story */
export const Cards = meta.story({
  render: (args) => {
    const customColor =
      args.customColor && args.customColor !== '#000000'
        ? args.customColor
        : undefined;

    const renderCard = (
      cardColor: 'elevated' | 'filled' | 'outlined',
      label: string,
      isSmall: boolean,
    ) => (
      <Card
        key={`${cardColor}-${isSmall ? 'small' : 'regular'}`}
        color={cardColor}
        disabled={args.cardDisabled}
        interactive={args.cardInteractive}
        className={
          isSmall
            ? 'w-[100px] h-[130px] relative overflow-hidden'
            : 'w-[200px] h-[264px] relative overflow-hidden'
        }>
        <Energy
          slot="container"
          active={args.energize}
          type={args.type}
          baseline={args.GM3}
          state={args.state}
          intensity={args.intensity}
          dynamicIntensity={args.dynamicIntensity}
          color={customColor}
        />
        <div className="relative z-1 p-4 typescale-body-md pointer-events-none">
          {label}
        </div>
      </Card>
    );

    return (
      <div className="space-y-6 p-6">
        <div className="flex flex-wrap items-start gap-4 pb-6">
          {renderCard('elevated', 'Elevated', true)}
          {renderCard('filled', 'Filled', true)}
          {renderCard('outlined', 'Outlined', true)}
        </div>
        <div className="flex flex-wrap items-start gap-4 pb-6">
          {renderCard('elevated', 'Elevated', false)}
          {renderCard('filled', 'Filled', false)}
          {renderCard('outlined', 'Outlined', false)}
        </div>
      </div>
    );
  },
});
