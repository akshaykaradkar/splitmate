// Storybook requires default exports
// tslint:disable:no-default-export

import preview from '../../../storybook/preview';
import {CircularProgress} from './circular-progress';
import {LinearProgress} from './linear-progress';
const meta = preview.meta({
  title: 'Components/Progress',
  parameters: {layout: 'padded'},
});

/** Storybook meta */
export default meta;

/** Linear story */
export const Linear = meta.story({
  args: {
    value: 75,
    buffer: 90,
  },
  argTypes: {
    value: {control: {type: 'range', min: 0, max: 100}},
    buffer: {control: {type: 'range', min: 0, max: 100}},
  },
  render: (args) => (
    <div className="flex flex-col gap-8 max-w-md">
      <div>
        <h4 className="md-typescale-label-md mb-4 text-on-surface-variant">
          Standard Determinate
        </h4>
        <LinearProgress
          value={args.value}
          buffer={args.buffer}
          variant="standard"
        />
      </div>
      <div>
        <h4 className="md-typescale-label-md mb-4 text-on-surface-variant">
          Standard Indeterminate
        </h4>
        <LinearProgress variant="standard" />
      </div>
      <div>
        <h4 className="md-typescale-label-md mb-4 text-on-surface-variant">
          Wavy Determinate
        </h4>
        <LinearProgress
          value={args.value}
          buffer={args.buffer}
          variant="wavy"
        />
      </div>
      <div>
        <h4 className="md-typescale-label-md mb-4 text-on-surface-variant">
          Wavy Indeterminate
        </h4>
        <LinearProgress variant="wavy" />
      </div>
    </div>
  ),
});

/** Circular story */
export const Circular = meta.story({
  args: {
    value: 75,
  },
  argTypes: {
    value: {control: {type: 'range', min: 0, max: 100}},
  },
  render: (args) => (
    <div className="flex gap-8">
      <div className="flex flex-col items-center gap-4">
        <span className="md-typescale-label-md text-on-surface-variant">
          Standard
        </span>
        <CircularProgress value={args.value} />
      </div>
      <div className="flex flex-col items-center gap-4">
        <span className="md-typescale-label-md text-on-surface-variant">
          Standard Indet.
        </span>
        <CircularProgress />
      </div>
      <div className="flex flex-col items-center gap-4">
        <span className="md-typescale-label-md text-on-surface-variant">
          Wavy
        </span>
        <CircularProgress value={args.value} variant="wavy" />
      </div>
      <div className="flex flex-col items-center gap-4">
        <span className="md-typescale-label-md text-on-surface-variant">
          Wavy Indet.
        </span>
        <CircularProgress variant="wavy" />
      </div>
    </div>
  ),
});
