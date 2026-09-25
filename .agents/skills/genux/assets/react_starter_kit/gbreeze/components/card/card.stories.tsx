// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {Button} from '../button/button';
import {Card} from './card';

const meta = preview.meta({
  title: 'Components/Card',
  component: Card,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    color: {control: 'select'},
    children: {table: {disable: true}},
  },
  args: {
    interactive: false,
    disabled: false,
    children: <div className="p-4">Card content</div>,
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({});

/** Colors story */
export const Colors = meta.story({
  argTypes: {
    color: {table: {disable: true}},
  },
  render: (args) => (
    <div className="flex gap-4">
      <Card {...args} color="elevated" />
      <Card {...args} color="filled" />
      <Card {...args} color="outlined" />
    </div>
  ),
});

/** Layouts story */
export const Layouts = meta.story({
  args: {
    color: 'elevated',
  },
  render: (args) => (
    <Card {...args} className="w-[360px]">
      <div className="h-48 bg-primary-container w-full" />
      <div className="p-4 flex flex-col flex-1 h-full">
        <h2 className="typescale-headline-sm mb-1">Headline</h2>
        <h3 className="typescale-body-lg mb-4">Subhead</h3>
        <p className="typescale-body-md mb-6">
          Explain more about the topic shown in the medium display and subhead
          through supporting text here.
        </p>
        <div className="mt-4 flex gap-2">
          <Button color="outlined" size="sm" label="Action" />
          <Button color="filled" size="sm" label="Action" />
        </div>
      </div>
    </Card>
  ),
});
