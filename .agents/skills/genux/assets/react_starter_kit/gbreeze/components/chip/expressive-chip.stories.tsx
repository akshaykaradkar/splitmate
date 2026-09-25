// Storybook requires default exports
// tslint:disable:no-default-export

import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {ExpressiveChip} from './expressive-chip';

const meta = preview.meta({
  title: 'Components/ExpressiveChip',
  component: ExpressiveChip,
  parameters: {
    layout: 'centered',
  },
  args: {
    label: 'Expressive Chip',
    color: 'outlined',
    type: 'action',
    selected: false,
    removable: false,
    disabled: false,
    softDisabled: false,
    onclick: fn(),
    onremove: fn(),
    onchange: fn(),
  },
  argTypes: {
    color: {
      control: 'select',
      options: ['elevated', 'filled', 'outlined', 'tonal'],
    },
    type: {
      control: 'select',
      options: ['action', 'filter', 'toggle', 'link'],
    },
    label: {control: 'text'},
    icon: {control: 'text'},
    selected: {control: 'boolean'},
    removable: {control: 'boolean'},
    disabled: {control: 'boolean'},
    softDisabled: {control: 'boolean'},
    onclick: {table: {disable: true}},
    onremove: {table: {disable: true}},
    onchange: {table: {disable: true}},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  args: {
    label: 'Expressive Chip',
    icon: 'star',
  },
});

/** Colors story */
export const Colors = meta.story({
  render: (args: any) => (
    <div className="flex flex-wrap gap-3">
      <ExpressiveChip {...args} color="elevated" label="Elevated" />
      <ExpressiveChip {...args} color="filled" label="Filled" />
      <ExpressiveChip {...args} color="outlined" label="Outlined" />
      <ExpressiveChip {...args} color="tonal" label="Tonal" />
    </div>
  ),
});

/** Types story */
export const Types = meta.story({
  render: (args: any) => (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-3 items-center">
        <span className="text-sm font-medium w-16">Action:</span>
        <ExpressiveChip
          {...args}
          type="action"
          label="Action"
          icon="play_arrow"
        />
      </div>
      <div className="flex flex-wrap gap-3 items-center">
        <span className="text-sm font-medium w-16">Filter:</span>
        <ExpressiveChip {...args} type="filter" label="Unselected" />
        <ExpressiveChip {...args} type="filter" label="Selected" selected />
      </div>
      <div className="flex flex-wrap gap-3 items-center">
        <span className="text-sm font-medium w-16">Toggle:</span>
        <ExpressiveChip {...args} type="toggle" label="Toggle off" />
        <ExpressiveChip {...args} type="toggle" label="Toggle on" selected />
      </div>
      <div className="flex flex-wrap gap-3 items-center">
        <span className="text-sm font-medium w-16">Link:</span>
        <ExpressiveChip
          {...args}
          type="link"
          label="Google"
          href="https://google.com"
          target="_blank"
          icon="open_in_new"
        />
      </div>
    </div>
  ),
});

/** Removable story */
export const Removable = meta.story({
  render: (args: any) => (
    <div className="flex flex-wrap gap-3">
      <ExpressiveChip {...args} removable label="Tag 1" color="elevated" />
      <ExpressiveChip {...args} removable label="Tag 2" color="filled" />
      <ExpressiveChip {...args} removable label="Tag 3" color="outlined" />
      <ExpressiveChip {...args} removable label="Tag 4" color="tonal" />
    </div>
  ),
});

/** IconsAndAvatar story */
export const IconsAndAvatar = meta.story({
  name: 'Icons / Avatar',
  render: (args: any) => (
    <div className="flex flex-wrap gap-3 items-center">
      <ExpressiveChip {...args} label="Favorite" icon="favorite" />
      <ExpressiveChip {...args} label="Bookmark" icon="bookmark" />
      <ExpressiveChip
        {...args}
        label="Avatar"
        avatar={
          <img
            src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=64&h=64&fit=crop&crop=faces"
            alt="User avatar"
            className="w-5 h-5 rounded-full object-cover"
          />
        }
      />
    </div>
  ),
});
