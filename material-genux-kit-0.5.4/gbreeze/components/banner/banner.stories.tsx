import preview from '../../../.storybook/preview';
import { Banner } from './banner';
import { Icon } from '../../styles/icons/icon';
import { Button } from '../button/button';

const meta = preview.meta({
  title: 'Components/Banner',
  component: Banner,
  args: {
    text: 'There is a problem with your payment method. Please update it to continue using our services.',
  },
  argTypes: {
    title: { control: 'text' },
    text: { control: 'text' },
    inset: { control: 'boolean' },
    centered: { control: 'boolean' },
    mobileStacked: { control: 'boolean' },
    graphic: { table: { disable: true } },
    actions: { table: { disable: true } },
    closeIcon: { table: { disable: true } },
  },
});

export default meta;

export const Playground = meta.story({
  args: {
    actions: (
      <>
        <Button color="text" label="Fix it" />
        <Button color="text" label="Learn more" />
      </>
    ),
  },
});

export const RichBanner = meta.story({
  name: 'Rich Information',
  args: {
    title: 'Update payment method',
    graphic: <Icon>credit_card</Icon>,
    actions: (
      <>
        <Button color="text" label="Fix it" />
        <Button color="text" label="Learn more" />
      </>
    ),
  },
});

export const Dismissible = meta.story({
  name: 'Dismissible',
  args: {
    title: 'Update payment method',
    graphic: <Icon>credit_card</Icon>,
    actions: (
      <>
        <Button color="text" label="Fix it" />
        <Button color="text" label="Learn more" />
      </>
    ),
    closeIcon: (
      <button className="icon-btn icon-btn-sm icon-btn-standard ripple focus-ring-outer" aria-label="Close">
        <Icon>close</Icon>
      </button>
    ),
  },
});

export const InsetBanner = meta.story({
  name: 'Inset (Rounded Corners)',
  args: {
    inset: true,
    title: 'Update payment method',
    text: 'There is a problem with your payment method.',
    graphic: <Icon>credit_card</Icon>,
    actions: (
      <>
        <Button color="text" label="Learn more" />
        <Button color="text" label="Fix it" />
      </>
    ),
  },
  parameters: {
    layout: 'padded',
  },
  render: (args: any) => (
    <div className="max-w-3xl mx-auto">
      <Banner {...args} />
    </div>
  ),
});

export const StackedActions = meta.story({
  name: 'Mobile Stacked Actions',
  args: {
    mobileStacked: true,
    title: 'Update payment method',
    text: 'Notice how the actions wrap to the bottom when the viewport is less than 600px wide.',
    actions: (
      <>
        <Button color="text" label="Learn more" />
        <Button color="text" label="Fix it" />
      </>
    ),
  },
  render: (args: any) => (
    <div className="w-[480px] border border-outline mx-auto m-8">
      <Banner {...args} />
    </div>
  ),
});
