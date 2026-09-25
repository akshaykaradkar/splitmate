// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {Icon} from '../../styles/icons/icon';
import {Badge} from './badge';

const meta = preview.meta({
  title: 'Components/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
  },
  args: {
    children: '3',
    dot: false,
  },
  argTypes: {
    value: {control: 'text'},
    dot: {control: 'boolean'},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  args: {
    children: '3',
  },
});

/** Small story */
export const Small = meta.story({
  name: 'Small (Dot)',
  args: {
    dot: true,
  },
});

/** Large story */
export const Large = meta.story({
  name: 'Large (Number)',
  args: {
    value: '12',
  },
});

/** MaxLimit story */
export const MaxLimit = meta.story({
  name: 'Max Limit Exceeded',
  args: {
    value: '99+',
  },
});

/** Anchored story */
export const Anchored = meta.story({
  name: 'Anchored to Icon',
  render: () => (
    <span style={{position: 'relative', display: 'inline-flex'}}>
      <Icon>mail</Icon>
      <Badge style={{position: 'absolute', top: -4, right: -4}}>5</Badge>
    </span>
  ),
});

/** AnchoredSmall story */
export const AnchoredSmall = meta.story({
  name: 'Anchored Small Dot',
  render: () => (
    <span style={{position: 'relative', display: 'inline-flex'}}>
      <Icon>notifications</Icon>
      <Badge dot style={{position: 'absolute', top: -2, right: -2}} />
    </span>
  ),
});
