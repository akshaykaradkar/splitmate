import addonA11y from '@storybook/addon-a11y';
import addonDocs from '@storybook/addon-docs';
import {definePreview} from '@storybook/react-vite';

// Include Tailwind and gBreeze styles
import '../src/index.css';

// tslint:disable-next-line:no-default-export Required by Storybook preview configuration.
export default definePreview({
  addons: [addonA11y(), addonDocs()],
  parameters: {
    backgrounds: {
      disable: true,
    },
    options: {
      storySort: {
        order: ['Design Tokens', 'Components', '*'],
      },
    },
  },
  decorators: [
    (Story, context) => {
      return (
        <>
          <style>{`
            :root {
              color-scheme: ${context.globals.colorScheme ?? 'light dark'};
              background-color: var(--md-sys-color-surface);
            }
          `}</style>
          <Story />
        </>
      );
    },
  ],
  globalTypes: {
    colorScheme: {
      description: 'Light and dark mode',
      dynamicTitle: true,
      toolbar: {
        title: 'Theme',
        icon: 'paintbrush',
        items: [
          {
            type: 'reset',
          },
          {
            value: 'light',
            title: 'Light',
            icon: 'sun',
          },
          {
            value: 'dark',
            title: 'Dark',
            icon: 'moon',
          },
        ],
      },
    },
  },
});
