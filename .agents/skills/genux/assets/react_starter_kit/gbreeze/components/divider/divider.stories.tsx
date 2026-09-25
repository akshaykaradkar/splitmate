// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {Divider} from './divider';

const meta = preview.meta({
  title: 'Components/Divider',
  component: Divider,
  parameters: {layout: 'padded'},
  args: {
    orientation: 'horizontal',
    inset: false,
    insetStart: false,
    insetEnd: false,
  },
  argTypes: {
    orientation: {control: 'select', options: ['horizontal', 'vertical']},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  render: (args) => (
    <div
      className={[
        'border',
        'border-outline',
        'rounded-md',
        'flex',
        args.orientation === 'vertical' ? 'flex-row h-16' : 'flex-col',
        'items-center',
        'justify-around',
      ].join(' ')}>
      <div className="p-4">Item 1</div>
      <Divider {...args} />
      <div className="p-4">Item 2</div>
      <Divider {...args} />
      <div className="p-4">Item 3</div>
    </div>
  ),
});
