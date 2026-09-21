import preview from '../../../.storybook/preview';
import { Tooltip } from './tooltip';
import { Button } from '../button/button';

const meta = preview.meta({
  title: 'Components/Tooltip',
  component: Tooltip,
  parameters: { layout: 'centered' },
  argTypes: {
    position: {
      control: 'select',
      options: ['top', 'bottom', 'left', 'right'],
    },
  },
});

export default meta;

export const Playground = meta.story({
  render: () => (
    <div className="h-[200px] flex items-center justify-center">
      <Button label="Hover or Click" popoverTarget="demo-tooltip" />
      <Tooltip id="demo-tooltip" position="top">
        This is a tooltip
      </Tooltip>
    </div>
  ),
});
