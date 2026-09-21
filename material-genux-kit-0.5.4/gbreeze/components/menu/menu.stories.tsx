/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { useRef } from 'react';
import { Menu, MenuItem, MenuGroup } from './menu';
import { Button } from '../button/button';
import { Divider } from '../divider/divider';

const meta = preview.meta({
  title: 'Components/Menu',
  component: Menu,
  parameters: { layout: 'centered' },
  argTypes: {
    color: { control: 'select', options: ['standard', 'vibrant'] },
    children: { table: { disable: true } },
  },
  args: {
    color: 'standard',
  },
});

export default meta;

export const Playground = meta.story({
  render: (args: any) => {
    const menuRef = useRef<HTMLElement>(null);
    return (
      <div className="h-[400px] flex flex-col items-center justify-start pt-8 gap-4">
        <Button
          label="Open Menu"
          onClick={() => menuRef.current?.togglePopover()}
          style={{ anchorName: `--menu-playground` }}
        />
        <Menu
          {...args}
          ref={menuRef}
          style={{ positionAnchor: `--menu-playground` }}
        >
          <MenuItem>Standard Item 1</MenuItem>
          <MenuItem supportingText="Supporting text">Standard Item 2</MenuItem>
          <MenuItem disabled>Standard Item 3</MenuItem>
          <Divider />
          <MenuGroup checkable="single">
            <MenuItem checked>Radio 1</MenuItem>
            <MenuItem>Radio 2</MenuItem>
            <MenuItem disabled>Radio 3</MenuItem>
          </MenuGroup>
          <Divider />
          <MenuGroup checkable="multiple">
            <MenuItem checked>Checkbox 1</MenuItem>
            <MenuItem>Checkbox 2</MenuItem>
            <MenuItem disabled checked>
              Checkbox 3
            </MenuItem>
          </MenuGroup>
        </Menu>
      </div>
    );
  },
});
