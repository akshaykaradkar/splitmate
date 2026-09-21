import { useId } from 'react';
import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { Menu, MenuItem } from '../menu/menu';
import { SplitButton } from './split-button';

const meta = preview.meta({
  title: 'Components/SplitButton',
  component: SplitButton,
  parameters: { layout: 'centered' },
  args: {
    label: 'Label',
    onClick: fn(),
  },
  argTypes: {
    label: { control: 'text' },
    size: { control: 'select', options: ['xs', 'sm', 'md', 'lg', 'xl'] },
    color: {
      control: 'select',
      options: ['filled', 'tonal', 'outlined', 'elevated'],
    },
    menuId: { table: { disable: true } },
    onClick: { table: { disable: true } },
    children: { table: { disable: true } },
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    label: 'Save',
  },
  render: (args) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const menuId = useId();
    return (
      <SplitButton {...args} menuId={menuId}>
        <Menu id={menuId}>
          <MenuItem label="Save and close" />
          <MenuItem label="Save as copy" />
        </Menu>
      </SplitButton>
    );
  },
});
