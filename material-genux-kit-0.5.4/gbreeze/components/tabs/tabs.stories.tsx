/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { Tabs, PrimaryTab, SecondaryTab } from './tabs';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/Tabs',
  component: Tabs,
  parameters: { layout: 'padded' },
});

export default meta;

export const Primary = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [activeTab, setActiveTab] = useState(0);

    return (
      <div className="w-full max-w-2xl mx-auto border border-outline rounded-xl mt-8">
        <Tabs
          activeTabIndex={activeTab}
          onchange={(e: any) => setActiveTab(e.target.activeTabIndex)}
        >
          <PrimaryTab icon="flight">Flights</PrimaryTab>
          <PrimaryTab icon="luggage">Trips</PrimaryTab>
          <PrimaryTab icon="explore">Explore</PrimaryTab>
        </Tabs>
        <div className="p-8 text-on-surface">
          {activeTab === 0 && <div>Book a flight here.</div>}
          {activeTab === 1 && <div>Manage your trips.</div>}
          {activeTab === 2 && <div>Find new destinations.</div>}
        </div>
      </div>
    );
  },
});

export const Secondary = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [activeTab, setActiveTab] = useState(0);

    return (
      <div className="w-full max-w-2xl mx-auto border border-outline rounded-xl mt-8">
        <Tabs
          activeTabIndex={activeTab}
          onchange={(e: any) => setActiveTab(e.target.activeTabIndex)}
        >
          <SecondaryTab>Item One</SecondaryTab>
          <SecondaryTab>Item Two</SecondaryTab>
          <SecondaryTab>Item Three</SecondaryTab>
        </Tabs>
        <div className="p-8 text-on-surface">
          {activeTab === 0 && <div>Content for item 1.</div>}
          {activeTab === 1 && <div>Content for item 2.</div>}
          {activeTab === 2 && <div>Content for item 3.</div>}
        </div>
      </div>
    );
  },
});
