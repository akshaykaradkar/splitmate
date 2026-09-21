import preview from '../../../.storybook/preview';
import { TopAppBar } from './app-bar';

const meta = preview.meta({
  title: 'Components/AppBar',
  component: TopAppBar,
  parameters: {
    layout: 'fullscreen',
  },
  args: {
    title: 'App Bar',
    variant: 'small',
    navigationIcon: 'menu',
    actionIcons: ['search', 'account_circle'],
    isScrolled: false,
  },
  argTypes: {
    variant: {
      control: 'select',
      options: ['center-aligned', 'small', 'medium', 'large'],
    },
    navigationIcon: { control: 'text' },
    isScrolled: { control: 'boolean' },
  },
});

export default meta;

export const Playground = meta.story({
  args: { title: 'App Bar' },
  render: (args) => (
    <div className="h-screen w-full bg-surface-container-lowest">
      <TopAppBar {...args} />
    </div>
  ),
});

export const Scrolled = meta.story({
  args: {
    title: 'Scrolled Bar',
    isScrolled: true,
  },
  render: (args) => (
    <div className="h-screen w-full bg-surface-container-lowest">
      <TopAppBar {...args} />
      <div className="p-8">
        <p className="md-typescale-body-lg text-on-surface-variant">The scrolled state elevates the app bar using a tonal color shift.</p>
      </div>
    </div>
  ),
});

export const Variants = meta.story({
  args: {
    title: 'App Bar',
  },
  argTypes: {
    variant: { table: { disable: true } },
    title: { table: { disable: true } },
  },
  parameters: {
    layout: 'padded',
  },
  render: (args) => (
    <div className="flex flex-col gap-8 w-full max-w-2xl mx-auto">
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <TopAppBar {...args} variant="small" title="Small" />
        <div className="p-4 h-32">Scroll content...</div>
      </div>
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <TopAppBar {...args} variant="center-aligned" title="Center Aligned" />
        <div className="p-4 h-32">Scroll content...</div>
      </div>
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <TopAppBar {...args} variant="medium" title="Medium" />
        <div className="p-4 h-32">Scroll content...</div>
      </div>
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <TopAppBar {...args} variant="large" title="Large" />
        <div className="p-4 h-32">Scroll content...</div>
      </div>
    </div>
  ),
});
