import preview from '../../../.storybook/preview';
import { FabMenu } from './fab-menu';

const meta = preview.meta({
  title: 'Components/FabMenu',
  component: FabMenu,
  parameters: { layout: 'centered' },
  args: {
    icon: 'add',
    activeIcon: 'close',
    variant: 'primary',
    actions: [
      {
        icon: 'edit',
        label: 'Edit',
        onClick: () => console.log('Edit clicked'),
      },
      {
        icon: 'delete',
        label: 'Delete',
        onClick: () => console.log('Delete clicked'),
      },
      {
        icon: 'share',
        label: 'Share',
        onClick: () => console.log('Share clicked'),
      },
    ],
  },
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'tertiary'],
    },
    size: { control: 'select', options: ['md', 'lg'] },
    actions: { table: { disable: true } },
  },
});

export default meta;

export const Primary = meta.story({});
export const Secondary = meta.story({ args: { variant: 'secondary' } });
export const Tertiary = meta.story({ args: { variant: 'tertiary' } });
export const Large = meta.story({ args: { size: 'lg' } });
