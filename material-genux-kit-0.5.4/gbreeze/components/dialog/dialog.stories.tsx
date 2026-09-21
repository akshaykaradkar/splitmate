import { fn } from 'storybook/test';
import preview from '../../../.storybook/preview';
import { Dialog } from './dialog';
import { Button } from '../button/button';

const meta = preview.meta({
  title: 'Components/Dialog',
  component: Dialog,
  parameters: { layout: 'centered' },
  args: {
    open: true,
    onclose: fn(),
    onclosed: fn(),
    onopen: fn(),
    onopened: fn(),
    oncancel: fn(),
  },
  argTypes: {
    open: { control: 'boolean' },
    headline: { control: 'text' },
    content: { control: 'text' },
    actions: { table: { disable: true } },
    onclose: { table: { disable: true } },
    onclosed: { table: { disable: true } },
    onopen: { table: { disable: true } },
    onopened: { table: { disable: true } },
    oncancel: { table: { disable: true } },
  },
});

export default meta;

export const WithProps = meta.story({
  args: {
    headline: 'Dialog Title',
    content:
      'This is the dialog body. It can contain text or other components.',
    actions: (formId: string) => (
      <>
        <Button formId={formId} value="cancel" color="text" label="Cancel" />
        <Button
          formId={formId}
          value="confirm"
          color="filled"
          label="Confirm"
        />
      </>
    ),
  },
});

export const WithChildren = meta.story({
  args: {
    children: (
      <>
        <div slot="headline" className="p-5 pb-0">
          Dialog Title
        </div>
        <form
          slot="content"
          id="children-form"
          method="dialog"
          className="p-5 pb-2"
        >
          This is the dialog body, rendered via children.
        </form>
        <div slot="actions" className="p-5 pt-4">
          <Button
            formId="children-form"
            value="cancel"
            color="text"
            label="Cancel"
          />
          <Button
            formId="children-form"
            value="confirm"
            color="filled"
            label="Confirm"
          />
        </div>
      </>
    ),
  },
});
