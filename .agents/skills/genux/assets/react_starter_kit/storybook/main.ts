import {defineMain} from '@storybook/react-vite/node';

// tslint:disable-next-line:no-default-export Required by Storybook main configuration.
export default defineMain({
  stories: ['../{src,gbreeze}/**/*.stories.@(ts|tsx)'],
  addons: ['@storybook/addon-a11y', '@storybook/addon-docs'],
  framework: '@storybook/react-vite',
  core: {
    allowedHosts: true,
  },
  viteFinal: async (config) => {
    config.base = './';
    return config;
  },
});
