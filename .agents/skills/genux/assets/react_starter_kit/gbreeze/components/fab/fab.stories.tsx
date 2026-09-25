// Storybook requires default exports
// tslint:disable:no-default-export

import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {Fab} from './fab';

const meta = preview.meta({
  title: 'Components/FAB',
  component: Fab,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    icon: {control: 'text'},
    label: {control: 'text'},
    color: {
      control: 'select',
      options: [
        'primary-container',
        'primary',
        'secondary-container',
        'secondary',
        'tertiary-container',
        'tertiary',
      ],
    },
    size: {control: 'select', options: ['default', 'md', 'lg']},
    children: {table: {disable: true}},
    onclick: {table: {disable: true}},
  },
  args: {
    icon: 'add',
    onclick: fn(),
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({});

/** ExtendedFAB story */
export const ExtendedFAB = meta.story({
  name: 'Extended FAB',
  args: {
    label: 'Add',
  },
});

/** Colors story */
export const Colors = meta.story({
  argTypes: {
    color: {table: {disable: true}},
  },
  render: (args) => (
    <div className="flex gap-4">
      <Fab {...args} color="primary-container" />
      <Fab {...args} color="secondary-container" />
      <Fab {...args} color="tertiary-container" />
      <Fab {...args} color="primary" />
      <Fab {...args} color="secondary" />
      <Fab {...args} color="tertiary" />
    </div>
  ),
});

/** Sizes story */
export const Sizes = meta.story({
  argTypes: {
    size: {table: {disable: true}},
  },
  render: (args) => (
    <div className="flex items-center gap-4">
      <Fab {...args} />
      <Fab {...args} size="md" />
      <Fab {...args} size="lg" />
    </div>
  ),
});
