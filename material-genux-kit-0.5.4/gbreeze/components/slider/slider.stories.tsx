/* eslint-disable react-hooks/rules-of-hooks */
import preview from '../../../.storybook/preview';
import { Slider } from './slider';
import { useState } from 'react';

const meta = preview.meta({
  title: 'Components/Slider',
  component: Slider,
  parameters: { layout: 'padded' },
});

export default meta;

export const Playground = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [val, setVal] = useState<number>(50);
    return (
      <div className="w-96 px-4">
        <Slider value={val} onChange={(e: any) => setVal(Number(e.target.value))} />
      </div>
    );
  },
});

export const WithTicks = meta.story({
  render: () => {
    // eslint-disable-next-react-hooks
    const [val, setVal] = useState<number>(20);
    return (
      <div className="w-96 px-4 py-8">
        <Slider 
          value={val} 
          min={0} 
          max={100} 
          step={20} 
          withTicks 
          withLabel 
          onChange={(e: any) => setVal(Number(e.target.value))} 
        />
      </div>
    );
  },
});
