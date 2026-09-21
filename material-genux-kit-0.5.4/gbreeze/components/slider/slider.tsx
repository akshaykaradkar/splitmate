/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef} from 'react';
import './slider.css';

export interface SliderProps
  extends Omit<ComponentPropsWithRef<'input'>, 'type'> {
  min?: number;
  max?: number;
  step?: number;
  value?: number;
  withLabel?: boolean;
  withTicks?: boolean;
}

export function Slider({
  min = 0,
  max = 100,
  step = 1,
  value,
  withLabel = false,
  withTicks = false,
  className = '',
  disabled,
  ...props
}: SliderProps) {
  // If value is controlled, use it; otherwise use internal state (not implemented here for simplicity, assuming controlled or default)
  const percent =
    value !== undefined ? ((value - min) / (max - min)) * 100 : 50;

  return (
    <div className={`slider ${className}`} aria-disabled={disabled}>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        disabled={disabled}
        className="slider-input"
        {...props}
      />

      {/* Track */}
      <div className="slider-track">
        <div className="slider-track-active" style={{width: `${percent}%`}} />
      </div>

      {/* Thumb */}
      <div className="slider-thumb" style={{left: `calc(${percent}% - 10px)`}}>
        {/* Hover state halo */}
        <div className="slider-halo" />
      </div>

      {/* Label (Tooltip) */}
      {withLabel && (
        <div className="slider-label" style={{left: `${percent}%`}}>
          {value}
        </div>
      )}

      {/* Ticks (Simplified) */}
      {withTicks && step && (
        <div className="slider-ticks">
          {Array.from({length: Math.floor((max - min) / step) + 1}).map(
            (_, i) => (
              <div
                key={i}
                className={`slider-tick ${
                  (i / ((max - min) / step)) * 100 <= percent
                    ? 'slider-tick-active'
                    : ''
                }`}
              />
            ),
          )}
        </div>
      )}
    </div>
  );
}
