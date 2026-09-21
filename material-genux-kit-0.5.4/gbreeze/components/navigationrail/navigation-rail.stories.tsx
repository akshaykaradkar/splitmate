/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { NavigationRail, NavigationRailItem } from './navigation-rail';
import { Icon } from '../../styles/icons/icon';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/NavigationRail',
  component: NavigationRail,
  parameters: { layout: 'fullscreen' },
});

export default meta;

export const Playground = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [active, setActive] = useState('Home');
    return (
      <div className="h-[600px] w-full bg-surface-container-lowest flex border-b border-outline">
        <NavigationRail
          header={
            <button className="icon-btn icon-btn-standard icon-btn-md ripple focus-ring-outer">
              <Icon>menu</Icon>
            </button>
          }
          fab={
            <button className="fab-tertiary-container shadow-elevation-1 rounded-xl p-4 ripple focus-ring-outer flex items-center justify-center">
              <Icon className="fab-icon">edit</Icon>
            </button>
          }
        >
          <NavigationRailItem
            icon="home"
            activeIcon="home"
            label="Home"
            active={active === 'Home'}
            onClick={() => setActive('Home')}
          />
          <NavigationRailItem
            icon="search"
            activeIcon="search"
            label="Search"
            active={active === 'Search'}
            onClick={() => setActive('Search')}
          />
          <NavigationRailItem
            icon="person"
            activeIcon="person"
            label="Profile"
            active={active === 'Profile'}
            onClick={() => setActive('Profile')}
          />
        </NavigationRail>
      </div>
    );
  },
});
