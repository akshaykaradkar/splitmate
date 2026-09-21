import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { IconButton } from './icon-button';

const meta = preview.meta({
  title: 'Components/Icon button',
  component: IconButton,
  parameters: {
    layout: 'centered',
  },
  args: {
    icon: 'spark',
    square: false,
    toggle: false,
    selected: false,
    onclick: fn(),
    onchange: fn(),
  },
  argTypes: {
    icon: { control: 'text' },
    color: {
      control: 'select',
      options: ['filled', 'tonal', 'outlined', 'standard'],
    },
    size: { control: 'select', options: ['xs', 'sm', 'md', 'lg', 'xl'] },
    width: { control: 'select', options: ['narrow', 'wide'] },
    selected: { if: { arg: 'toggle' } },
    children: { table: { disable: true } },
    onclick: { table: { disable: true } },
    onchange: { table: { disable: true } },
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    color: 'filled',
  },
});

export const Colors = meta.story({
  argTypes: {
    color: { table: { disable: true } },
  },
  render: (args) => (
    <div className="flex gap-4">
      <IconButton color="filled" {...args} />
      <IconButton color="tonal" {...args} />
      <IconButton color="outlined" {...args} />
      <IconButton color="standard" {...args} />
    </div>
  ),
});

export const Sizes = meta.story({
  args: {
    color: 'filled',
  },
  argTypes: {
    size: { table: { disable: true } },
  },
  render: (args) => (
    <div className="flex items-center gap-4">
      <IconButton {...args} size="xs" />
      <IconButton {...args} size="sm" />
      <IconButton {...args} size="md" />
      <IconButton {...args} size="lg" />
      <IconButton {...args} size="xl" />
    </div>
  ),
});

export const Widths = meta.story({
  args: {
    color: 'filled',
    size: 'md',
  },
  argTypes: {
    width: { table: { disable: true } },
  },
  render: (args) => (
    <div className="flex items-center gap-4">
      <IconButton {...args} width="narrow" />
      <IconButton {...args} />
      <IconButton {...args} width="wide" />
    </div>
  ),
});
