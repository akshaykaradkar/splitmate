// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {Icon} from '../../styles/icons/icon';
import {IconButton} from '../iconbutton/icon-button';
import {AppBar} from './app-bar';

const meta = preview.meta({
  title: 'Components/AppBar',
  component: AppBar,
  parameters: {
    layout: 'fullscreen',
  },
  args: {
    title: 'App Bar',
    size: 'sm',
    variant: 'standard',
    scrolled: false,
  },
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    variant: {
      control: 'select',
      options: ['standard', 'search'],
    },
    scrolled: {control: 'boolean'},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  args: {
    title: 'App Bar',
    size: 'sm',
    variant: 'standard',
    scrolled: false,
  },
  render: (args) => (
    <div className="h-screen w-full bg-surface-container-lowest">
      <AppBar
        {...args}
        leading={<IconButton icon="menu" />}
        trailing={
          <>
            <IconButton icon="search" />
            <IconButton icon="account_circle" />
          </>
        }
      />
    </div>
  ),
});

/** Scrolled story */
export const Scrolled = meta.story({
  args: {
    title: 'Scrolled Bar',
    scrolled: true,
  },
  render: (args) => (
    <div className="h-screen w-full bg-surface-container-lowest">
      <AppBar
        {...args}
        leading={<IconButton icon="menu" />}
        trailing={<IconButton icon="more_vert" />}
      />
      <div className="p-8">
        <p className="md-typescale-body-lg text-on-surface-variant">
          The scrolled state elevates the app bar using a tonal color shift.
        </p>
      </div>
    </div>
  ),
});

/** Sizes story */
export const Sizes = meta.story({
  args: {
    title: 'App Bar',
  },
  argTypes: {
    size: {table: {disable: true}},
    title: {table: {disable: true}},
  },
  parameters: {
    layout: 'padded',
  },
  render: (args) => (
    <div className="flex flex-col gap-8 w-full max-w-2xl mx-auto">
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <AppBar
          {...args}
          size="sm"
          title="Small"
          leading={<IconButton icon="menu" />}
          trailing={<IconButton icon="more_vert" />}
        />
        <div className="p-4 h-24">Content below small bar</div>
      </div>
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <AppBar
          {...args}
          size="md"
          title="Medium"
          subtitle="Supporting subtitle text"
          leading={<IconButton icon="arrow_back" />}
          trailing={<IconButton icon="more_vert" />}
        />
        <div className="p-4 h-24">Content below medium bar</div>
      </div>
      <div className="border border-outline overflow-hidden rounded-xl bg-surface-container-lowest">
        <AppBar
          {...args}
          size="lg"
          title="Large"
          subtitle="Supporting subtitle text"
          leading={<IconButton icon="arrow_back" />}
          trailing={
            <>
              <IconButton icon="attach_file" />
              <IconButton icon="calendar_today" />
              <IconButton icon="more_vert" />
            </>
          }
        />
        <div className="p-4 h-24">Content below large bar</div>
      </div>
    </div>
  ),
});

/** Search story */
export const Search = meta.story({
  args: {
    variant: 'search',
  },
  render: (args) => (
    <div className="h-screen w-full bg-surface-container-lowest">
      <AppBar
        {...args}
        variant="search"
        leading={<IconButton icon="menu" />}
        search={
          <div className="flex items-center gap-2 w-full px-4 py-2 bg-surface-container-high rounded-full">
            <Icon>search</Icon>
            <input
              type="text"
              placeholder="Search..."
              className="bg-transparent border-none outline-none w-full text-on-surface"
            />
          </div>
        }
        trailing={<IconButton icon="account_circle" />}
      />
    </div>
  ),
});
