import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import {
  AssistChip,
  FilterChip,
  InputChip,
  SuggestionChip,
  ChipSet,
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
    label: { control: 'text' },
    icon: { control: 'text' },
    onclick: { table: { disable: true } },
    ['onremove' as any]: { table: { disable: true } },
  },
});

export default meta;

export const Assist = meta.story({
  render: (args: any) => (
    <ChipSet>
      <AssistChip {...args} label="Add to calendar" icon="event" />
      <AssistChip {...args} label="Set alarm" icon="alarm" />
      <AssistChip {...args} label="Remind me" icon="notifications" />
    </ChipSet>
  ),
});

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

export const Suggestion = meta.story({
  render: (args: any) => (
    <ChipSet>
      <SuggestionChip {...args} label="I agree" />
      <SuggestionChip {...args} label="Sounds good" />
      <SuggestionChip {...args} label="Maybe later" />
    </ChipSet>
  ),
});
