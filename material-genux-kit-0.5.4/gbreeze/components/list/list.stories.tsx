/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import radioStyles from '@material/web/labs/gb/components/radio/radio.css' with { type: 'css' };
import checkboxStyles from '@material/web/labs/gb/components/checkbox/checkbox.css' with { type: 'css' };
import { adoptStyles } from '@material/web/labs/gb/styles/adopt-styles';
import { List, ListItem } from './list';
import { useList, useListItem } from './list-hooks';
import React from 'react';

adoptStyles(document, [radioStyles, checkboxStyles]);

const meta = preview.meta({
  title: 'Components/List',
  component: List,
  parameters: { layout: 'centered' },
  argTypes: {
    children: { table: { disable: true } },
  },
  args: {
    segmented: true,
  },
});

export default meta;

// Wrapper component to apply container styles
const Container = ({ children }: { children: React.ReactNode }) => (
  <div className="container" style={{ width: '420px' }}>
    <style>{`
      .container {
        background-color: var(--md-sys-color-surface-container);
        border-radius: var(--md-sys-shape-corner-lg);
        padding: 2rem;
        border: 1px solid var(--md-sys-color-outline-variant);
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
    `}</style>
    {children}
  </div>
);

export const Playground = meta.story({
  args: {
    nonInteractive: false,
  },
  render: (args: any) => (
    <Container>
      <List segmented={args.segmented}>
        <ListItem nonInteractive={args.nonInteractive} label="Basic Item" />
        <ListItem
          nonInteractive={args.nonInteractive}
          leadingContent={<span className="md-icon">star</span>}
          label="With Leading Icon"
        />
        <ListItem
          nonInteractive={args.nonInteractive}
          avatar="A"
          label="With Avatar & Supporting Text"
          supportingText="Supporting text goes here"
        />
        <ListItem
          nonInteractive={args.nonInteractive}
          style={{ alignItems: 'start' }}
          leadingContent={<span className="md-icon">image</span>}
          supportingText="With overline, support text, and two icons"
          trailingText="100+"
          trailingContent={<span className="md-icon">chevron_right</span>}
        >
          <span slot="overline">Overline text</span>
          Complex Item
        </ListItem>
        <ListItem
          nonInteractive={args.nonInteractive}
          checked
          leadingContent={<span className="md-icon">check</span>}
          label="Selected Item"
        />
        <ListItem
          nonInteractive={args.nonInteractive}
          disabled
          leadingContent={<span className="md-icon">block</span>}
          label="Disabled Item"
          supportingText="This item is disabled"
        />
      </List>
    </Container>
  ),
});

export const NonInteractive = meta.story({
  name: 'Non-interactive',
  render: (args: any) => (
    <Container>
      <List segmented={args.segmented}>
        <ListItem
          nonInteractive
          leadingContent={<span className="md-icon">developer_board</span>}
          trailingText="Feb 14, 1946"
          label="The first computer (ENIAC)"
        />
        <ListItem
          nonInteractive
          leadingContent={<span className="md-icon">satellite_alt</span>}
          trailingText="Oct 4, 1957"
          label="Sputnik launched into space"
        />
        <ListItem
          nonInteractive
          leadingContent={<span className="md-icon">rocket_launch</span>}
          trailingText="Jul 20, 1969"
          label="The Apollo 11 moon landing"
        />
        <ListItem
          nonInteractive
          leadingContent={<span className="md-icon">moon_stars</span>}
          trailingText="Apr 1, 2026"
          label="Artemis 2 crewed lunar flyby"
        />
      </List>
    </Container>
  ),
});

export const SingleAction = meta.story({
  name: 'Single-action',
  render: (args: any) => (
    <Container>
      <style>{`
        .img {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          background-color: var(--md-sys-color-tertiary-container);
        }
      `}</style>
      <List segmented={args.segmented}>
        <ListItem
          leadingContent={<div className="img" slot="avatar"></div>}
          supportingText="Food, music, arts, community..."
          trailingText="May 8"
          label="Festivals"
        />
        <ListItem
          leadingContent={<div className="img" slot="avatar"></div>}
          supportingText="Literature, games, music, physical..."
          trailingText="May 8"
          label="Arts"
        />
        <ListItem
          leadingContent={<div className="img" slot="avatar"></div>}
          supportingText="The relationships that bring and bind..."
          trailingText="May 8"
          label="Family & friends"
        />
      </List>
    </Container>
  ),
});

export const SingleSelect = meta.story({
  name: 'Single-select',
  args: {
    segmented: false,
  },
  render: (args: any) => {
    const listRef = React.useRef<HTMLDivElement>(null);
    const listClassesStr = useList(listRef, args);

    const item1Ref = React.useRef<HTMLOptionElement>(null);
    const item2Ref = React.useRef<HTMLOptionElement>(null);
    const item3Ref = React.useRef<HTMLOptionElement>(null);

    return (
      <Container>
        <select className="list-select" size={3} defaultValue="2">
          <div ref={listRef} className={listClassesStr}>
            <option ref={item1Ref} className={useListItem(item1Ref)} value="1">
              <div className="list-item-leading list-item-radio radio"></div>
              List item 1
            </option>
            <option ref={item2Ref} className={useListItem(item2Ref)} value="2">
              <div className="list-item-leading list-item-radio radio"></div>
              List item 2
            </option>
            <option ref={item3Ref} className={useListItem(item3Ref)} value="3">
              <div className="list-item-leading list-item-radio radio"></div>
              List item 3
            </option>
          </div>
        </select>
      </Container>
    );
  },
});

export const MultiSelect = meta.story({
  name: 'Multi-select',
  args: {
    segmented: false,
  },
  render: (args: any) => {
    const listRef = React.useRef<HTMLDivElement>(null);
    const listClassesStr = useList(listRef, args);

    const item1Ref = React.useRef<HTMLOptionElement>(null);
    const item2Ref = React.useRef<HTMLOptionElement>(null);
    const item3Ref = React.useRef<HTMLOptionElement>(null);

    return (
      <Container>
        <select className="list-select" multiple size={3} defaultValue={['2']}>
          <div ref={listRef} className={listClassesStr}>
            <option ref={item1Ref} className={useListItem(item1Ref)} value="1">
              <div className="list-item-leading list-item-checkbox checkbox"></div>
              List item 1
            </option>
            <option ref={item2Ref} className={useListItem(item2Ref)} value="2">
              <div className="list-item-leading list-item-checkbox checkbox"></div>
              List item 2
            </option>
            <option ref={item3Ref} className={useListItem(item3Ref)} value="3">
              <div className="list-item-leading list-item-checkbox checkbox"></div>
              List item 3
            </option>
          </div>
        </select>
      </Container>
    );
  },
});
