// Storybook requires default exports
// tslint:disable:no-default-export

import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {
  AssistChip,
  ChipSet,
  FilterChip,
  InputChip,
  SuggestionChip,
} from './chip';

const meta = preview.meta({
  title: 'Components/Chip',
  component: AssistChip,
  parameters: {
    layout: 'centered',
  },
  args: {
    label: 'Chip text',
    elevated: false,
    disabled: false,
    onclick: fn(),
    ['onremove' as any]: fn(),
  },
  argTypes: {
    label: {control: 'text'},
    icon: {control: 'text'},
    onclick: {table: {disable: true}},
    ['onremove' as any]: {table: {disable: true}},
  },
});

/** Storybook meta */
export default meta;

/** Assist story */
export const Assist = meta.story({
  render: (args: any) => (
    <ChipSet>
      <AssistChip {...args} label="Add to calendar" icon="event" />
      <AssistChip {...args} label="Set alarm" icon="alarm" />
      <AssistChip {...args} label="Remind me" icon="notifications" />
    </ChipSet>
  ),
});

/** Filter story */
export const Filter = meta.story({
  render: (args: any) => (
    <ChipSet>
      <FilterChip {...args} label="Docs" selected />
      <FilterChip {...args} label="Slides" />
      <FilterChip {...args} label="Sheets" selected />
      <FilterChip {...args} label="Images" />
    </ChipSet>
  ),
});

/** Input story */
export const Input = meta.story({
  render: (args: any) => (
    <ChipSet>
      <InputChip {...args} label="Apple" />
      <InputChip
        {...args}
        label="Banana"
        icon={<img src="https://i.pravatar.cc/150?img=1" />}
        avatar
      />
      <InputChip {...args} label="Cherry" />
    </ChipSet>
  ),
});

/** Suggestion story */
export const Suggestion = meta.story({
  render: (args: any) => (
    <ChipSet>
      <SuggestionChip {...args} label="I agree" />
      <SuggestionChip {...args} label="Sounds good" />
      <SuggestionChip {...args} label="Maybe later" />
    </ChipSet>
  ),
});
