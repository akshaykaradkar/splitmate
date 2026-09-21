import { defineMain } from '@storybook/react-vite/node';

export default defineMain({
  stories: ['../(src|gbreeze)/**/*.stories.@(ts|tsx)'],
  addons: ['@storybook/addon-a11y', '@storybook/addon-docs'],
  framework: '@storybook/react-vite',
});
