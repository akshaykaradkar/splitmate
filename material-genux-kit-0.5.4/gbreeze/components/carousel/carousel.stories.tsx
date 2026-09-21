import preview from '../../../.storybook/preview';
import { Carousel } from './carousel';

const sampleItems = [
  {
    id: '1',
    image: 'https://images.unsplash.com/photo-1707343843437-caacff5cfa74?w=800&q=80',
    title: 'Mountain Retreat',
    subtitle: 'Escape to the peaks',
  },
  {
    id: '2',
    image: 'https://images.unsplash.com/photo-1473580044384-7ba9967e16a0?w=800&q=80',
    title: 'Desert Dunes',
    subtitle: 'Explore the vastness',
  },
  {
    id: '3',
    image: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80',
    title: 'Ocean Breeze',
    subtitle: 'Feel the waves',
  },
  {
    id: '4',
    image: 'https://picsum.photos/seed/forest/800/800',
    title: 'Forest Trail',
    subtitle: 'Walk among giants',
  },
  {
    id: '5',
    image: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&q=80',
    title: 'City Lights',
    subtitle: 'Urban exploration',
  },
];

const meta = preview.meta({
  title: 'Components/Carousel',
  component: Carousel,
  args: {
    items: sampleItems,
  },
  argTypes: {
    variant: {
      control: 'select',
      options: ['multi-browse', 'hero', 'full-screen'],
    },
    items: { table: { disable: true } },
  },
});

export default meta;

export const MultiBrowse = meta.story({
  name: 'Multi-Browse (Default)',
  args: {
    variant: 'multi-browse',
  },
  parameters: {
    layout: 'padded',
  },
});

export const Hero = meta.story({
  name: 'Hero Carousel',
  args: {
    variant: 'hero',
  },
  parameters: {
    layout: 'padded',
  },
  render: (args: any) => (
    <div className="max-w-4xl mx-auto">
      <Carousel {...args} />
    </div>
  ),
});

export const FullScreen = meta.story({
  name: 'Full-Screen Immersion',
  args: {
    variant: 'full-screen',
  },
  parameters: {
    layout: 'fullscreen',
  },
});
