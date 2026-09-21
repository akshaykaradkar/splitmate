/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { NavigationBar, NavigationBarItem } from './navigation-bar';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/NavigationBar',
  component: NavigationBar,
  parameters: { layout: 'fullscreen' },
});

export default meta;

export const Playground = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [active, setActive] = useState('Home');
    return (
      <div className="h-[500px] w-full bg-surface-container-lowest flex flex-col justify-end border-b border-outline">
        <NavigationBar>
          <NavigationBarItem
            icon="home"
            activeIcon="home"
            label="Home"
            active={active === 'Home'}
            onClick={() => setActive('Home')}
          />
          <NavigationBarItem
            icon="search"
            activeIcon="search"
            label="Search"
            active={active === 'Search'}
            onClick={() => setActive('Search')}
          />
          <NavigationBarItem
            icon="person"
            activeIcon="person"
            label="Profile"
            active={active === 'Profile'}
            onClick={() => setActive('Profile')}
          />
        </NavigationBar>
      </div>
    );
  },
});
