import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { Switch } from './switch';

const meta = preview.meta({
  title: 'Components/Switch',
  component: Switch,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    onIcon: { control: 'text' },
    offIcon: { control: 'text' },
    onclick: { table: { disable: true } },
    onchange: { table: { disable: true } },
    oninput: { table: { disable: true } },
  },
  args: {
    disabled: false,
    selected: false,
    onIcon: '',
    offIcon: '',
    onclick: fn(),
    onchange: fn(),
    oninput: fn(),
  },
});

export default meta;

export const Playground = meta.story({});

export const WithIcons = meta.story({
  name: 'With icons',
  args: {
    onIcon: 'check',
    offIcon: 'close',
  },
});
