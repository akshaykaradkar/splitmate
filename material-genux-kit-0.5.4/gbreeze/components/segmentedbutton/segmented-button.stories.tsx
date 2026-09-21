/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { SegmentedButton } from './segmented-button';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/SegmentedButton',
  component: SegmentedButton,
  parameters: { layout: 'centered' },
  args: {
    options: [
      { value: 'day', label: 'Day' },
      { value: 'week', label: 'Week' },
      { value: 'month', label: 'Month' },
    ],
  },
});

export default meta;

export const SingleSelect = meta.story({
  render: (args: any) => {
    // eslint-disable-next-react-hooks
    const [val, setVal] = useState<string[]>(['week']);
    return <SegmentedButton {...args} value={val} onChange={setVal} />;
  },
});

export const MultiSelect = meta.story({
  args: {
    multiSelect: true,
    options: [
      { value: 'bold', label: '', icon: 'format_bold' },
      { value: 'italic', label: '', icon: 'format_italic' },
      { value: 'underline', label: '', icon: 'format_underlined' },
    ],
  },
  render: (args: any) => {
    // eslint-disable-next-react-hooks
    const [val, setVal] = useState<string[]>(['bold']);
    return <SegmentedButton {...args} value={val} onChange={setVal} />;
  },
});
