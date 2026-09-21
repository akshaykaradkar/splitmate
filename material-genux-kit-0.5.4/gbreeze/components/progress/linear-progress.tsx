/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated, based on GM3-Wiz implementation */
import {ComponentPropsWithRef, useEffect, useRef} from 'react';
import './linear-progress.css';

// --- Linear Constants ---
const LINEAR_TRANSITION_DURATION_MS = 250;
const LINEAR_SMOOTHING_FACTOR = 0.5;
const LINEAR_RESOLUTION = 1; // Pixels
const LINEAR_AMPLITUDE_DEFAULT = 3;
const LINEAR_WAVELENGTH_DEFAULT = 30;
const LINEAR_WAVESPEED_DEFAULT = 30;

// Cubic Bezier helper for ease-out
const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);

// --- Interfaces ---

export interface LinearProgressProps extends ComponentPropsWithRef<'div'> {
  value?: number; // 0 to 100. If undefined, indeterminate.
  buffer?: number;
  variant?: 'standard' | 'wavy';
}

// --- Standard Components ---

function StandardLinearProgress({
  value,
  buffer = 100,
  className = '',
  ...props
}: LinearProgressProps) {
  const rootRef = useRef<HTMLDivElement>(null);
  const isIndeterminate = value === undefined;

  const progressValue =
    value !== undefined ? Math.min(Math.max(value / 100, 0), 1) : 0;
  const bufferValue = Math.min(Math.max(buffer / 100, 0), 1);


  useEffect(() => {
    if (!rootRef.current || !isIndeterminate) return;

    // Animation percentages from GM3 specification
    const ANIMATION_PERCENTAGES = {
      PRIMARY_HALF: 0.8367142,
      PRIMARY_FULL: 2.00611057,
      SECONDARY_QUARTER: 0.37651913,
      SECONDARY_HALF: 0.84386165,
      SECONDARY_FULL: 1.60277782,
    };

    // Calculate node proportions into CSS Custom Properties
    const width = rootRef.current.offsetWidth;
    const primaryHalf = width * ANIMATION_PERCENTAGES.PRIMARY_HALF;
    const primaryFull = width * ANIMATION_PERCENTAGES.PRIMARY_FULL;
    const secondaryQuarter = width * ANIMATION_PERCENTAGES.SECONDARY_QUARTER;
    const secondaryHalf = width * ANIMATION_PERCENTAGES.SECONDARY_HALF;
    const secondaryFull = width * ANIMATION_PERCENTAGES.SECONDARY_FULL;

    const style = rootRef.current.style;
    style.setProperty('--mdc-linear-progress-primary-half', `${primaryHalf}px`);
    style.setProperty(
      '--mdc-linear-progress-primary-half-neg',
      `${-primaryHalf}px`,
    );
    style.setProperty('--mdc-linear-progress-primary-full', `${primaryFull}px`);
    style.setProperty(
      '--mdc-linear-progress-primary-full-neg',
      `${-primaryFull}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-quarter',
      `${secondaryQuarter}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-quarter-neg',
      `${-secondaryQuarter}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-half',
      `${secondaryHalf}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-half-neg',
      `${-secondaryHalf}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-full',
      `${secondaryFull}px`,
    );
    style.setProperty(
      '--mdc-linear-progress-secondary-full-neg',
      `${-secondaryFull}px`,
    );
  }, [isIndeterminate]);

  const containerClasses = ['progress-linear-wrapper', className]
    .filter(Boolean)
    .join(' ');

  const linearProgressClasses = [
    'progress-linear',
    isIndeterminate
      ? 'progress-linear--indeterminate progress-linear--animation-ready'
      : '',
  ]
    .filter(Boolean)
    .join(' ');

  const trackLeftClasses = isIndeterminate
    ? 'progress-linear__track-left'
    : 'progress-linear__active-indicator';
  const trackMiddleClasses = isIndeterminate
    ? 'progress-linear__active-indicator'
    : 'progress-linear__track';
  const trackRightClasses = isIndeterminate
    ? 'progress-linear__track-right'
    : 'progress-linear__buffer-bar';

  return (
    <div
      ref={rootRef}
      className={containerClasses}
      data-progressvalue={progressValue}
      data-buffervalue={bufferValue}
      {...props}>
      <div
        className={linearProgressClasses}
        role="progressbar"
        {...(!isIndeterminate
          ? {
              'aria-valuenow': progressValue,
              'aria-valuemin': 0,
              'aria-valuemax': 1,
            }
          : {})}>
        <div
          className={trackLeftClasses}
          style={!isIndeterminate ? {minWidth: `${progressValue * 100}%`} : {}}
        />
        <div
          className="progress-linear__gap-left"
          style={
            !isIndeterminate ? {flexGrow: progressValue === 0 ? 0 : 1} : {}
          }
        />
        <div className={trackMiddleClasses} />
        <div
          className="progress-linear__gap-right"
          style={
            !isIndeterminate
              ? {
                  display:
                    (progressValue === 0 && bufferValue === 0) ||
                    bufferValue === 1
                      ? 'none'
                      : 'block',
                }
              : {}
          }
        />
        <div
          className={trackRightClasses}
          style={
            !isIndeterminate ? {width: `${(1 - bufferValue) * 100}%`} : {}
          }>
          <div className="progress-linear__buffer-dots" />
        </div>
        <div className="progress-linear__stop-indicator" />
      </div>
    </div>
  );
}

// --- Wavy Components ---

function WavyLinearProgress({
  value,
  className = '',
  ...props
}: LinearProgressProps) {
  const rootRef = useRef<HTMLDivElement>(null);
  const leftPathRef = useRef<SVGPathElement>(null);
  const middlePathRef = useRef<SVGPathElement>(null);
  const rightPathRef = useRef<SVGPathElement>(null);

  const isIndeterminate = value === undefined;
  const isDeterminate = value !== undefined;
  const progressValue =
    value !== undefined ? Math.min(Math.max(value / 100, 0), 1) : 0;

  // Animation State
  const state = useRef({
    animationStart: 0,
    animationTarget: 0,
    animationCurrent: 0,
    amplitudeCurrent: 0,
    timeCurrent: 0,
    timeRemaining: 0,
    wavelengthOffset: 0,
    activeCached: 0,
    activeIndicatorData: '',
    amplitude: LINEAR_AMPLITUDE_DEFAULT,
    wavelength: LINEAR_WAVELENGTH_DEFAULT,
    wavespeed: LINEAR_WAVESPEED_DEFAULT,
  });

  useEffect(() => {
    const s = state.current;

    // Initial setup
    s.activeIndicatorData = '';
    s.activeCached = 0;

    if (isDeterminate) {
      if (rightPathRef.current) rightPathRef.current.removeAttribute('d');
      s.animationTarget = progressValue;
      s.animationStart = s.animationCurrent;
      s.timeRemaining = LINEAR_TRANSITION_DURATION_MS;
      s.wavespeed = 0;

      if (value === 100) {
        if (rootRef.current) {
          const rect = rootRef.current.getBoundingClientRect();
          const length = rect.width;
          const style = window.getComputedStyle(rootRef.current);
          const strokeWidth =
            parseFloat(style.getPropertyValue('stroke-width')) || 4;
          const gapSize = parseFloat(style.getPropertyValue('min-height')) || 4;
          const effectiveLength = length - 2 * strokeWidth - gapSize;
          if (effectiveLength > 0)
            s.animationTarget = (length - strokeWidth) / effectiveLength;
        }
      } else if (value === 0) {
        if (rootRef.current) {
          const rect = rootRef.current.getBoundingClientRect();
          const length = rect.width;
          const style = window.getComputedStyle(rootRef.current);
          const strokeWidth =
            parseFloat(style.getPropertyValue('stroke-width')) || 4;
          const gapSize = parseFloat(style.getPropertyValue('min-height')) || 4;
          const effectiveLength = length - 2 * strokeWidth - gapSize;
          if (effectiveLength > 0)
            s.animationTarget = (-1 * strokeWidth - gapSize) / effectiveLength;
        }
      }
    } else {
      s.wavespeed = LINEAR_WAVESPEED_DEFAULT;
      if (rootRef.current && middlePathRef.current && leftPathRef.current) {
        const amplitudeOriginal = s.amplitudeCurrent;
        s.amplitudeCurrent = s.amplitude;
        const rect = rootRef.current.getBoundingClientRect();
        const length = rect.width;
        const style = window.getComputedStyle(rootRef.current);
        const strokeWidth =
          parseFloat(style.getPropertyValue('stroke-width')) || 4;
        const xStart = Math.round(strokeWidth / 2);
        const xEnd = Math.round(length - strokeWidth / 2);

        let dActive = '';
        const xCached = 0;
        const center = rect.height / 2;

        for (
          let x = Math.max(xStart, xCached);
          x < xEnd;
          x += LINEAR_RESOLUTION
        ) {
          const amplitudeOffset =
            s.amplitudeCurrent *
            Math.sin((2 * Math.PI * x) / s.wavelength + s.wavelengthOffset);
          const power =
            1 -
            (s.amplitudeCurrent - Math.abs(amplitudeOffset)) /
              Math.max(s.amplitudeCurrent, s.amplitude / 2);
          const smoothingOffset =
            Math.sign(amplitudeOffset) *
            LINEAR_SMOOTHING_FACTOR *
            (s.amplitudeCurrent - Math.abs(amplitudeOffset)) *
            power ** 2;
          const y = center + amplitudeOffset + smoothingOffset;

          if (dActive === '') {
            dActive += `M ${x} ${y} `;
          } else {
            dActive += `L ${x} ${y} `;
          }
        }
        middlePathRef.current.setAttribute('d', dActive);

        let trackData = '';
        if (xEnd > xStart) {
          trackData += `M ${xStart} ${center} `;
          trackData += `L ${xEnd} ${center} `;
        }
        leftPathRef.current.setAttribute('d', trackData);
        if (rightPathRef.current)
          rightPathRef.current.setAttribute('d', trackData);

        s.amplitudeCurrent = amplitudeOriginal;

        const activePathLength = middlePathRef.current.getTotalLength();
        rootRef.current.style.setProperty(
          '--active-path-length',
          `${activePathLength}px`,
        );
        const trackPathLength = leftPathRef.current.getTotalLength();
        rootRef.current.style.setProperty(
          '--track-path-length',
          `${trackPathLength}px`,
        );
      }
    }

    s.timeCurrent = performance.now();
  }, [progressValue, isDeterminate, value]);

  useEffect(() => {
    let animationFrameId: number;

    const draw = () => {
      if (
        !rootRef.current ||
        !leftPathRef.current ||
        !middlePathRef.current ||
        !rightPathRef.current
      )
        return;

      if (!isDeterminate) return; // Handled purely via CSS when indeterminate in GM3

      const s = state.current;

      const isInProgress =
        (s.amplitudeCurrent !== 0 && s.amplitudeCurrent !== s.amplitude) ||
        s.animationCurrent !== s.animationTarget ||
        s.wavespeed > 0;

      const rect = rootRef.current.getBoundingClientRect();
      const length = rect.width;

      if (length === 0) {
        if (isInProgress) animationFrameId = requestAnimationFrame(draw);
        return;
      }

      const style = window.getComputedStyle(rootRef.current);
      const strokeWidth =
        parseFloat(style.getPropertyValue('stroke-width')) || 4;
      const gapSize = parseFloat(style.getPropertyValue('min-height')) || 4;
      const effectiveLength = length - 2 * strokeWidth - gapSize;
      const center = rect.height / 2;

      const now = performance.now();
      const elapsedTime = now - s.timeCurrent;

      s.wavelengthOffset =
        (s.wavelengthOffset +
          (2 * Math.PI * s.wavespeed * elapsedTime) / 1000) %
        (2 * Math.PI);

      // Active indicator calculations
      const activeStart = Math.round(strokeWidth / 2);
      const activeEnd = Math.round(
        activeStart + s.animationCurrent * effectiveLength,
      );

      if (
        s.animationTarget < s.animationCurrent ||
        s.amplitudeCurrent !== s.amplitude ||
        s.wavespeed > 0
      ) {
        s.activeIndicatorData = '';
        s.activeCached = 0;
      }

      for (
        let x = Math.max(activeStart, s.activeCached);
        x < activeEnd;
        x += LINEAR_RESOLUTION
      ) {
        const amplitudeOffset =
          s.amplitudeCurrent *
          Math.sin((2 * Math.PI * x) / s.wavelength + s.wavelengthOffset);
        const power =
          1 -
          (s.amplitudeCurrent - Math.abs(amplitudeOffset)) /
            Math.max(s.amplitudeCurrent, s.amplitude / 2);
        const smoothingOffset =
          Math.sign(amplitudeOffset) *
          LINEAR_SMOOTHING_FACTOR *
          (s.amplitudeCurrent - Math.abs(amplitudeOffset)) *
          power ** 2;
        const y = center + amplitudeOffset + smoothingOffset;

        if (s.activeIndicatorData === '') {
          s.activeIndicatorData += `M ${x} ${y} `;
        } else {
          s.activeIndicatorData += `L ${x} ${y} `;
        }
      }
      s.activeCached = activeEnd;
      leftPathRef.current.setAttribute('d', s.activeIndicatorData);

      // Track calculations
      const trackStart = Math.round(activeEnd + strokeWidth + gapSize);
      const trackEnd = Math.round(length - strokeWidth / 2);

      let trackData = '';
      if (trackEnd > trackStart) {
        trackData += `M ${trackStart} ${center} `;
        trackData += `L ${trackEnd} ${center} `;
      }
      middlePathRef.current.setAttribute('d', trackData);

      // Transition Logic
      if (s.timeRemaining > 0) {
        const progress =
          (LINEAR_TRANSITION_DURATION_MS - s.timeRemaining) /
          LINEAR_TRANSITION_DURATION_MS;
        // using easeOut cubic bezier sharp as per typical MD3, previously mapped to 'sharp'
        s.animationCurrent =
          easeOut(progress) * (s.animationTarget - s.animationStart) +
          s.animationStart;
        s.timeRemaining -= elapsedTime;
      } else {
        s.animationCurrent = s.animationTarget;
      }

      if (s.animationCurrent >= 0.1 && s.animationCurrent < 0.95) {
        s.amplitudeCurrent +=
          s.amplitude * (elapsedTime / LINEAR_TRANSITION_DURATION_MS);
        s.amplitudeCurrent = Math.min(s.amplitudeCurrent, s.amplitude);
      } else {
        s.amplitudeCurrent -=
          s.amplitude * (elapsedTime / LINEAR_TRANSITION_DURATION_MS);
        s.amplitudeCurrent = Math.max(s.amplitudeCurrent, 0);
      }

      if (isInProgress) {
        s.timeCurrent = now;
        animationFrameId = requestAnimationFrame(draw);
      }
    };

    if (isDeterminate) {
      animationFrameId = requestAnimationFrame(draw);
    }
    return () => cancelAnimationFrame(animationFrameId);
  }, [isDeterminate, value]);

  const containerClasses = ['progress-linear-wavy-wrapper', className]
    .filter(Boolean)
    .join(' ');

  const linearProgressClasses = [
    'progress-linear-wavy',
    isIndeterminate ? 'progress-linear--wavy--indeterminate' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const trackLeftClasses = isIndeterminate
    ? 'progress-linear-wavy__track-left'
    : 'progress-linear-wavy__active-indicator';
  const trackMiddleClasses = isIndeterminate
    ? 'progress-linear-wavy__active-indicator'
    : 'progress-linear-wavy__track';
  const trackRightClasses = 'progress-linear-wavy__track-right';

  return (
    <div
      ref={rootRef}
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
      className={containerClasses}
      data-progressvalue={progressValue}
      data-amplitude={LINEAR_AMPLITUDE_DEFAULT}
      data-wavelength={LINEAR_WAVELENGTH_DEFAULT}
      data-wavespeed={LINEAR_WAVESPEED_DEFAULT}
      {...props}>
      <div className={linearProgressClasses}>
        <svg className={trackLeftClasses} xmlns="http://www.w3.org/2000/svg">
          <path ref={leftPathRef} d="" />
        </svg>
        <svg className={trackMiddleClasses} xmlns="http://www.w3.org/2000/svg">
          <path ref={middlePathRef} d="" />
        </svg>
        <svg className={trackRightClasses} xmlns="http://www.w3.org/2000/svg">
          <path ref={rightPathRef} d="" />
        </svg>
        <div className="progress-linear__stop-indicator" />
      </div>
    </div>
  );
}

// --- Exports ---

export function LinearProgress({
  variant = 'standard',
  ...props
}: LinearProgressProps) {
  if (variant === 'wavy') {
    return <WavyLinearProgress {...props} />;
  }
  return <StandardLinearProgress {...props} />;
}
