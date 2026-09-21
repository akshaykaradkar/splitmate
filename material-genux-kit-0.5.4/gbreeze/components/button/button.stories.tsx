import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { Button } from './button';

const meta = preview.meta({
  title: 'Components/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
  args: {
    label: 'Label',
    disabled: false,
    softDisabled: false,
    toggle: false,
    selected: false,
    square: false,
    onclick: fn(),
    onchange: fn(),
  },
  argTypes: {
    color: {
      control: 'select',
      options: ['filled', 'elevated', 'tonal', 'outlined', 'text'],
    },
    size: { control: 'select', options: ['xs', 'sm', 'md', 'lg', 'xl'] },
    selected: { if: { arg: 'toggle' } },
    label: { control: 'text' },
    icon: { control: 'text' },
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
    label: { table: { disable: true } },
  },
  render: (args) => (
    <div className="flex gap-4">
      <Button {...args} color="filled" label="Filled" />
      <Button {...args} color="elevated" label="Elevated" />
      <Button {...args} color="tonal" label="Tonal" />
      <Button {...args} color="outlined" label="Outlined" />
      {!args.toggle && <Button {...args} color="text" label="Text" />}
    </div>
  ),
});

export const Sizes = meta.story({
  args: {
    color: 'filled',
  },
  argTypes: {
    size: { table: { disable: true } },
    label: { table: { disable: true } },
  },
  render: (args) => (
    <div className="flex items-center gap-4">
      <Button {...args} size="xs" label="XSmall" />
      <Button {...args} size="sm" label="Small" />
      <Button {...args} size="md" label="Medium" />
      <Button {...args} size="lg" label="Large" />
      <Button {...args} size="xl" label="XLarge" />
    </div>
  ),
});
