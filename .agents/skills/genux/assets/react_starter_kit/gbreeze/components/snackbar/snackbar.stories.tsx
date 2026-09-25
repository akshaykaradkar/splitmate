// Storybook requires default exports
// tslint:disable:no-default-export

/* eslint-disable react-hooks/rules-of-hooks */
/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {useEffect, useState} from 'react';
import {fn} from 'storybook/test';
import preview from '../../../storybook/preview';
import {Button} from '../button/button';
import {Snackbar} from './snackbar';

const meta = preview.meta({
  title: 'Components/Snackbar',
  component: Snackbar,
  parameters: {
    layout: 'centered',
  },
  args: {
    message: 'Single line message with action and close.',
    actionLabel: 'Action',
    showClose: true,
    isOpen: true,
    twoLine: false,
    timeoutMs: 0,
    onAction: fn(),
    onClose: fn(),
  },
  argTypes: {
    message: {control: 'text'},
    actionLabel: {control: 'text'},
    showClose: {control: 'boolean'},
    isOpen: {control: 'boolean'},
    twoLine: {control: 'boolean'},
    timeoutMs: {control: 'number'},
    children: {table: {disable: true}},
    onAction: {action: 'actionClicked'},
    onClose: {action: 'closed'},
    onDismiss: {action: 'dismissed'},
  },
});

/** Storybook meta */
export default meta;

/** Playground story */
export const Playground = meta.story({
  render: (args: any) => {
    const [open, setOpen] = useState(args.isOpen ?? true);

    useEffect(() => {
      setOpen(args.isOpen ?? true);
    }, [args.isOpen]);

    return (
      <div className="flex flex-col items-center justify-center gap-6 min-h-[160px] p-6">
        <Button
          label={open ? 'Hide Snackbar' : 'Show Snackbar'}
          onClick={() => setOpen(!open)}
        />
        <Snackbar
          {...args}
          isOpen={open}
          onClose={() => {
            args.onClose?.();
            setOpen(false);
          }}
          onAction={() => {
            args.onAction?.();
            setOpen(false);
          }}
          style={{position: 'static'}}
        />
      </div>
    );
  },
});

export const SingleLine = meta.story({
  render: (args: any) => (
    <div className="p-6 flex justify-center items-center">
      <Snackbar
        {...args}
        message="Single line snackbar message."
        action={undefined}
        actionLabel={undefined}
        showClose={false}
        isOpen={true}
        twoLine={false}
        style={{position: 'static'}}
      />
    </div>
  ),
});

export const SingleLineWithAction = meta.story({
  render: (args: any) => (
    <div className="p-6 flex justify-center items-center">
      <Snackbar
        {...args}
        message="Single line message with action."
        actionLabel="Action"
        showClose={false}
        isOpen={true}
        twoLine={false}
        style={{position: 'static'}}
      />
    </div>
  ),
});

export const SingleLineWithActionAndClose = meta.story({
  render: (args: any) => (
    <div className="p-6 flex justify-center items-center">
      <Snackbar
        {...args}
        message="Single line message with action and close."
        actionLabel="Action"
        showClose={true}
        isOpen={true}
        twoLine={false}
        style={{position: 'static'}}
      />
    </div>
  ),
});

export const TwoLine = meta.story({
  render: (args: any) => (
    <div className="p-6 flex justify-center items-center">
      <Snackbar
        {...args}
        message="This is a two-line snackbar notification that provides more detailed information to the user."
        action={undefined}
        actionLabel={undefined}
        showClose={false}
        isOpen={true}
        twoLine={true}
        style={{position: 'static'}}
      />
    </div>
  ),
});

export const TwoLineWithAction = meta.story({
  render: (args: any) => (
    <div className="p-6 flex justify-center items-center">
      <Snackbar
        {...args}
        message="This is a two-line snackbar with an action button for user follow-up."
        actionLabel="Action"
        showClose={false}
        isOpen={true}
        twoLine={true}
        style={{position: 'static'}}
      />
    </div>
  ),
});
