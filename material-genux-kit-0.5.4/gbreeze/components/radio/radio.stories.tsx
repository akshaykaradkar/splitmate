import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { Radio } from './radio';

const meta = preview.meta({
  title: 'Components/Radio',
  component: Radio,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    onchange: { table: { disable: true } },
    oninput: { table: { disable: true } },
  },
  args: {
    disabled: false,
    onchange: fn(),
    oninput: fn(),
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    checked: false,
  },
});

export const WithLabel = meta.story({
  name: 'With label',
  render: (args) => (
    <div className="flex flex-col gap-4">
      <label className="flex items-center gap-2">
        <Radio {...args} name="radio-group" />
        Option one
      </label>
      <label className="flex items-center gap-2">
        <Radio {...args} name="radio-group" />
        Option two
      </label>
      <label className="flex items-center gap-2">
        <Radio {...args} name="radio-group" />
        Option three
      </label>
    </div>
  ),
});
