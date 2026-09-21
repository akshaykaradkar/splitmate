import preview from '../../../.storybook/preview';
import { Badge } from './badge';
import { Icon } from '../../styles/icons/icon';

const meta = preview.meta({
  title: 'Components/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    children: '3',
  },
});

export const Small = meta.story({
  name: 'Small (Dot)',
});

export const Large = meta.story({
  name: 'Large (Number)',
  args: {
    children: '12',
  },
});

export const MaxLimit = meta.story({
  name: 'Max Limit Exceeded',
  args: {
    children: '99+',
  },
});

export const Anchored = meta.story({
  name: 'Anchored to Icon',
  render: () => (
    <span style={{ anchorName: '--badge' } as React.CSSProperties}>
      <Icon>mail</Icon>
      <Badge>5</Badge>
    </span>
  ),
});

export const AnchoredSmall = meta.story({
  name: 'Anchored Small Dot',
  render: () => (
    <span style={{ anchorName: '--badge' } as React.CSSProperties}>
      <Icon>notifications</Icon>
      <Badge />
    </span>
  ),
});
