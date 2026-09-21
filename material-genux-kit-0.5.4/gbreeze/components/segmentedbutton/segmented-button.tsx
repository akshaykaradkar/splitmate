/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef} from 'react';
import './segmented-button.css';
import { Icon } from '../../styles/icons/icon';

export interface SegmentedButtonOption {
  value: string;
  label: string;
  icon?: string;
  disabled?: boolean;
}

export interface SegmentedButtonProps
  extends Omit<ComponentPropsWithRef<'div'>, 'onChange'> {
  options: SegmentedButtonOption[];
  value?: string[]; // Array for multi-select, single string for single-select (handled by logic)
  onChange?: (value: string[]) => void;
  multiSelect?: boolean;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

export function SegmentedButton({
  options,
  value = [],
  onChange,
  multiSelect = false,
  className = '',
  ...props
}: SegmentedButtonProps) {
  const handleToggle = (optionValue: string) => {
    let newValue: string[];
    if (multiSelect) {
      if (value.includes(optionValue)) {
        newValue = value.filter((v) => v !== optionValue);
      } else {
        newValue = [...value, optionValue];
      }
    } else {
      newValue = [optionValue];
    }
    onChange?.(newValue);
  };

  return (
    <div
      className={`segmented-button ${className ?? ''}`}
      role="group"
      {...props}>
      {options.map((option) => {
        const isSelected = value.includes(option.value);

        return (
          <button
            key={option.value}
            disabled={option.disabled}
            onClick={() => handleToggle(option.value)}
            aria-pressed={isSelected}
            className="segmented-button-button ripple focus-ring-inner"
          >
            {isSelected && <Icon className="segmented-button-icon">check</Icon>}
            {!isSelected && option.icon && (
              <Icon className="segmented-button-icon">{option.icon}</Icon>
            )}
            <span>{option.label}</span>
          </button>
        );
      })}
    </div>
  );
}
