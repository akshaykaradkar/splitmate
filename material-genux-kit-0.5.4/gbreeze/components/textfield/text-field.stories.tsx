import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { TextField } from './text-field';

const meta = preview.meta({
  title: 'Components/TextField',
  component: TextField,
  parameters: { layout: 'centered' },
  args: {
    disabled: false,
    error: false,
    errorText: '',
    onchange: fn(),
    oninput: fn(),
  },
  argTypes: {
    variant: { control: 'select', options: ['filled', 'outlined'] },
    onchange: { table: { disable: true } },
    oninput: { table: { disable: true } },
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    label: 'First Name',
    supportingText: 'Enter your legal first name',
    variant: 'filled',
  },
});

export const Outlined = meta.story({
  args: {
    label: 'Username',
    variant: 'outlined',
    supportingText: 'Enter your username',
  },
});

export const WithError = meta.story({
  args: {
    label: 'Email',
    variant: 'filled',
    type: 'email',
    error: true,
    errorText: 'Invalid email address',
  },
});

export const WithPrefixAndSuffix = meta.story({
  args: {
    label: 'Price',
    variant: 'outlined',
    prefixText: '$',
    suffixText: '.00',
  },
});
