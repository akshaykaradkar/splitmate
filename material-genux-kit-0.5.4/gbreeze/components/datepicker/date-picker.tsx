/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, useState} from 'react';
import './date-picker.css';
import { Icon } from '../../styles/icons/icon';

export interface DatePickerProps extends ComponentPropsWithRef<'div'> {
  onCancel?: () => void;
  onConfirm?: (date: Date) => void;
}

export function DatePicker({
  onCancel,
  onConfirm,
  className = '',
  ...props
}: DatePickerProps) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  const daysInMonth = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth() + 1,
    0,
  ).getDate();
  const firstDayOfMonth = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth(),
    1,
  ).getDay();

  const handlePrevMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() - 1),
    );
  };

  const handleNextMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() + 1),
    );
  };

  const handleDateClick = (day: number) => {
    const newDate = new Date(
      currentDate.getFullYear(),
      currentDate.getMonth(),
      day,
    );
    setSelectedDate(newDate);
  };

  const isSelected = (day: number) => {
    return (
      selectedDate?.getDate() === day &&
      selectedDate?.getMonth() === currentDate.getMonth() &&
      selectedDate?.getFullYear() === currentDate.getFullYear()
    );
  };

  const isToday = (day: number) => {
    const today = new Date();
    return (
      today.getDate() === day &&
      today.getMonth() === currentDate.getMonth() &&
      today.getFullYear() === currentDate.getFullYear()
    );
  };

  return (
    <div className={`date-picker ${className ?? ''}`} {...props}>
      {/* Header */}
      <div className="date-picker-header">
        <div className="date-picker-header-title">Select date</div>
        <div className="date-picker-header-date">
          {selectedDate
            ? selectedDate.toLocaleDateString('en-US', {
                weekday: 'short',
                month: 'short',
                day: 'numeric',
              })
            : 'Select a date'}
        </div>
      </div>

      {/* Controls */}
      <div className="date-picker-controls">
        <div className="date-picker-controls-month">
          {currentDate.toLocaleDateString('en-US', {
            month: 'long',
            year: 'numeric',
          })}
        </div>
        <div className="date-picker-controls-nav">
          <button
            onClick={handlePrevMonth}
            className="icon-btn icon-btn-standard icon-btn-sm ripple focus-ring-outer"
          >
            <Icon>chevron_left</Icon>
          </button>
          <button
            onClick={handleNextMonth}
            className="icon-btn icon-btn-standard icon-btn-sm ripple focus-ring-outer"
          >
            <Icon>chevron_right</Icon>
          </button>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="date-picker-calendar">
        <div className="date-picker-weekdays">
          {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((day, i) => (
            <div key={i} className="date-picker-weekday">
              {day}
            </div>
          ))}
        </div>
        <div className="date-picker-days">
          {Array.from({ length: firstDayOfMonth }).map((_, i) => (
            <div key={`empty-${i}`} />
          ))}
          {Array.from({ length: daysInMonth }).map((_, i) => {
            const day = i + 1;
            const selected = isSelected(day);
            const today = isToday(day);
            return (
              <div key={day} className="date-picker-day-container">
                <button
                  onClick={() => handleDateClick(day)}
                  className={`date-picker-day ripple
                    ${selected ? 'date-picker-day-selected' : ''}
                    ${today && !selected ? 'date-picker-day-today' : ''}
                  `}
                >
                  {day}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* Actions */}
      <div className="date-picker-actions">
        <button
          className="btn btn-text btn-sm ripple focus-ring-outer"
          onClick={onCancel}
        >
          Cancel
        </button>
        <button
          className="btn btn-text btn-sm ripple focus-ring-outer"
          onClick={() => selectedDate && onConfirm?.(selectedDate)}
          disabled={!selectedDate}
        >
          OK
        </button>
      </div>
    </div>
  );
}
