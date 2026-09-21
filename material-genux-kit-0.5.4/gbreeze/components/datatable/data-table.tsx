/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {
  ComponentPropsWithRef,
  ReactNode,
  useEffect,
  useRef,
  useState,
} from 'react';
import './data-table.css';

export interface DataTableColumn<T> {
  key: keyof T;
  header: string;
  align?: 'left' | 'right' | 'center';
  render?: (value: T[keyof T], item: T) => ReactNode;
}

export interface DataTableProps<T> extends ComponentPropsWithRef<'div'> {
  columns: DataTableColumn<T>[];
  data: T[];
  selectable?: boolean;
  onSelectionChange?: (selectedIndices: number[]) => void;
}

export function DataTable<T>({
  columns,
  data,
  selectable = false,
  onSelectionChange,
  className = '',
  ...props
}: DataTableProps<T>) {
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const headerCheckboxRef = useRef<HTMLInputElement>(null);

  const toggleAll = () => {
    if (selected.size === data.length) {
      setSelected(new Set());
      onSelectionChange?.([]);
    } else {
      const newSelected = new Set(data.map((_, i) => i));
      setSelected(newSelected);
      onSelectionChange?.(Array.from(newSelected));
    }
  };

  const toggleRow = (index: number) => {
    const newSelected = new Set(selected);
    if (newSelected.has(index)) {
      newSelected.delete(index);
    } else {
      newSelected.add(index);
    }
    setSelected(newSelected);
    onSelectionChange?.(Array.from(newSelected));
  };

  const isAllSelected = data.length > 0 && selected.size === data.length;
  const isIndeterminate = selected.size > 0 && selected.size < data.length;

  useEffect(() => {
    if (headerCheckboxRef.current) {
      headerCheckboxRef.current.indeterminate = isIndeterminate;
    }
  }, [headerCheckboxRef, isIndeterminate]);

  return (
    <div className={`data-table ${className ?? ''}`} {...props}>
      <div className="data-table-scroll">
        <table>
          <thead>
            <tr>
              {selectable && (
                <th className="data-table-cell-checkbox">
                  <div>
                    <input
                      ref={headerCheckboxRef}
                      type="checkbox"
                      className="checkbox ripple focus-ring-outer"
                      checked={isAllSelected}
                      onChange={toggleAll}
                    />
                  </div>
                </th>
              )}
              {columns.map((col) => (
                <th
                  key={String(col.key)}
                  className={`data-table-cell text-${col.align || 'left'}`}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row, i) => (
              <tr
                key={i}
                className={`data-table-row transition-colors ${selected.has(i) ? 'selected' : ''}`}>
                {selectable && (
                  <td className="data-table-cell-checkbox">
                    <div>
                      <input
                        type="checkbox"
                        className="checkbox ripple focus-ring-outer"
                        checked={selected.has(i)}
                        onChange={() => toggleRow(i)}
                      />
                    </div>
                  </td>
                )}
                {columns.map((col) => (
                  <td
                    key={String(col.key)}
                    className={`data-table-cell text-${col.align || 'left'}`}>
                    {col.render
                      ? col.render(row[col.key], row)
                      : String(row[col.key])}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
