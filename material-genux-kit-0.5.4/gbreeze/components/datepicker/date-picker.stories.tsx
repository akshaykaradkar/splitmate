import preview from '../../../.storybook/preview';
import { DatePicker } from './date-picker';

const meta = preview.meta({
  title: 'Components/DatePicker',
  component: DatePicker,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    onCancel: { action: 'cancelled' },
    onConfirm: { action: 'confirmed' },
  },
});

export default meta;

export const Playground = meta.story({});
