import preview from '../../../.storybook/preview';
import { DataTable } from './data-table';

interface ExampleData {
  id: number;
  name: string;
  role: string;
  status: string;
}

const tableData: ExampleData[] = [
  { id: 1, name: 'Alice', role: 'Engineer', status: 'Active' },
  { id: 2, name: 'Bob', role: 'Designer', status: 'Active' },
  { id: 3, name: 'Charlie', role: 'Manager', status: 'Inactive' },
];

const meta = preview.meta({
  title: 'Components/DataTable',
  component: DataTable,
  parameters: {
    layout: 'padded',
  },
  args: {
    columns: [
      { key: 'name', header: 'Name' },
      { key: 'role', header: 'Role' },
      { key: 'status', header: 'Status' },
    ],
    data: tableData,
    selectable: false,
  },
});

export default meta;

export const Playground = meta.story({});

export const Selectable = meta.story({
  args: { selectable: true },
});
