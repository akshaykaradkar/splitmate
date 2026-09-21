import preview from '../../../.storybook/preview';
import { Toolbar, ToolbarButton, ToolbarSeparator } from './toolbar';

const meta = preview.meta({
  title: 'Components/Toolbar',
  component: Toolbar,
  parameters: { layout: 'padded' },
});

export default meta;

export const Playground = meta.story({
  render: () => (
    <div className="flex justify-center mt-8">
      <Toolbar>
        <ToolbarButton icon="format_bold" />
        <ToolbarButton icon="format_italic" active />
        <ToolbarButton icon="format_underlined" />
        <ToolbarSeparator />
        <ToolbarButton icon="format_align_left" active />
        <ToolbarButton icon="format_align_center" />
        <ToolbarButton icon="format_align_right" />
      </Toolbar>
    </div>
  ),
});
