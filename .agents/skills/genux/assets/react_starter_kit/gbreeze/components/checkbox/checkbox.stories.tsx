// Storybook requires default exports
// tslint:disable:no-default-export

import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {Checkbox} from './checkbox';

const meta = preview.meta({
  title: 'Components/Checkbox',
  component: Checkbox,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    checked: {control: 'boolean'},
    onclick: {table: {disable: true}},
    oninput: {table: {disable: true}},
    onchange: {table: {disable: true}},
  },
  args: {
    indeterminate: false,
    disabled: false,
    error: false,
    onclick: fn(),
    oninput: fn(),
    onchange: fn(),
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story();

/** WithLabel story */
export const WithLabel = meta.story({
  name: 'With label',
  render: (args) => (
    <label className="flex items-center gap-2">
      <Checkbox {...args} />
      Label
    </label>
  ),
});
