// Storybook requires default exports
// tslint:disable:no-default-export

import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {Slider} from './slider';

const meta = preview.meta({
  title: 'Components/Slider',
  component: Slider,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    min: {control: 'number'},
    max: {control: 'number'},
    value: {control: 'number'},
    valueStart: {control: 'number'},
    valueEnd: {control: 'number'},
    step: {control: 'number'},
    ticks: {control: 'boolean'},
    labeled: {control: 'boolean'},
    range: {control: 'boolean'},
    disabled: {control: 'boolean'},
    onchange: {table: {disable: true}},
    onChange: {table: {disable: true}},
    oninput: {table: {disable: true}},
    onInput: {table: {disable: true}},
  },
  args: {
    min: 0,
    max: 100,
    value: 50,
    disabled: false,
    ticks: false,
    labeled: false,
    range: false,
    onchange: fn(),
    oninput: fn(),
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  render: (args) => (
    <div className="w-80">
      <Slider {...args} />
    </div>
  ),
});

/** WithTicksAndLabels story */
export const WithTicksAndLabels = meta.story({
  name: 'With ticks and labels',
  args: {
    min: 0,
    max: 100,
    value: 50,
    step: 10,
    ticks: true,
    labeled: true,
  },
  render: (args) => (
    <div className="w-80">
      <Slider {...args} />
    </div>
  ),
});

/** Range story */
export const Range = meta.story({
  name: 'Range',
  args: {
    min: 0,
    max: 100,
    range: true,
    valueStart: 25,
    valueEnd: 75,
    labeled: true,
  },
  render: (args) => (
    <div className="w-80">
      <Slider {...args} />
    </div>
  ),
});
