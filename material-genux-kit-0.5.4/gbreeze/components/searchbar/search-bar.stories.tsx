import preview from '../../../.storybook/preview';
import { SearchBar } from './search-bar';

const meta = preview.meta({
  title: 'Components/SearchBar',
  component: SearchBar,
  parameters: { layout: 'padded' },
  args: {
    placeholder: 'Hinted search text',
    leadingIcon: 'search',
  },
  argTypes: {
    onTrailingIconClick: { action: 'trailingIconClicked' },
  },
});

export default meta;

export const Playground = meta.story({});

export const WithTrailingIcon = meta.story({
  args: {
    trailingIcon: 'mic',
    placeholder: 'Search our directory',
  },
  render: (args: any) => (
    <div className="max-w-xl mx-auto">
      <SearchBar {...args} />
    </div>
  ),
});
