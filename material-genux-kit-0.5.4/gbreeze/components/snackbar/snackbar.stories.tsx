/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { Snackbar } from './snackbar';
import { Button } from '../button/button';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/Snackbar',
  component: Snackbar,
  parameters: { layout: 'fullscreen' },
  argTypes: {
    onAction: { action: 'actionClicked' },
    onDismiss: { action: 'dismissed' },
  },
});

export default meta;

export const Playground = meta.story({
  render: (args: any) => {
    // eslint-disable-next-react-hooks
    const [open, setOpen] = useState(true);
    return (
      <div className="h-[300px] w-full flex items-center justify-center relative bg-surface-container-lowest">
        <Button label="Show Snackbar" onClick={() => setOpen(true)} />
        <div className="absolute top-4 left-4 right-4 flex justify-center z-[100]">
          <Snackbar 
            {...args} 
            isOpen={open} 
            message="Your message was sent successfully."
            action="Undo"
            showClose={true}
            onDismiss={() => {
              args.onDismiss?.();
              setOpen(false);
            }} 
            onAction={() => {
              args.onAction?.();
              setOpen(false);
            }}
          />
        </div>
      </div>
    );
  },
});
