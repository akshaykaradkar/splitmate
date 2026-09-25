/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated. Implementation matching GM3 Circular Progress spec */

import {ComponentPropsWithRef, CSSProperties, useEffect, useRef} from 'react';
import './circular-progress.css';

/** Props for the CircularProgress component. */
export interface CircularProgressProps extends ComponentPropsWithRef<'div'> {
  value?: number; // 0 to 100. If undefined, indeterminate.
  variant?: 'standard' | 'wavy';
  // Wavy specific props
  amplitude?: number;
  wavelength?: number;
  wavespeed?: number;
  closed?: boolean;
}

/** Material 3 CircularProgress component supporting determinate, indeterminate, and wavy styles. */
export function CircularProgress({
  value,
  variant = 'standard',
  className = '',
  style,
  amplitude = 1.6,
  wavelength = 15,
  wavespeed = 0,
  closed = false,
  ...rest
}: CircularProgressProps) {
  const isIndeterminate = value === undefined;
  const progressValue = isIndeterminate
    ? 0
    : Math.max(0, Math.min(100, value)) / 100;

  const isWavy = variant === 'wavy';

  const graphicRef = useRef<HTMLDivElement>(null);
  const activeIndicatorRef = useRef<SVGPathElement & SVGCircleElement>(null);
  const trackRef = useRef<SVGPathElement & SVGCircleElement>(null);
  const amplitudeRef = useRef<number>(
    isIndeterminate || (progressValue >= 0.1 && progressValue < 0.95)
      ? amplitude
      : 0,
  );

  // Derived Classes
  let stateClasses = '';
  if (isIndeterminate) stateClasses += ' indeterminate';
  if (closed) stateClasses += ' closed';
  if (!isIndeterminate) {
    if (progressValue === 1) stateClasses += ' complete';
    else if (progressValue > 0.9) stateClasses += ' almost-complete';
    else if (progressValue === 0) stateClasses += ' unopened';
  }

  // Effect for Wavy mathematical rendering
  useEffect(() => {
    if (
      !isWavy ||
      !activeIndicatorRef.current ||
      !trackRef.current ||
      !graphicRef.current
    ) {
      return;
    }

    let animationFrameId: number;
    let timeCurrent = performance.now();
    let wavelengthOffset = 0;

    const graphicEl = graphicRef.current;
    const activeIndicator = activeIndicatorRef.current as SVGPathElement;
    const track = trackRef.current as SVGPathElement;

    // Use resize observer or just read once
    const strokeWidth =
      Number(
        getComputedStyle(graphicEl).strokeWidth.replace(/px$/i, '').trim(),
      ) || 4;
    const rect = graphicEl.getBoundingClientRect();
    const width = rect.width || 48;
    const center = width / 2;
    const radius = (1.5 * center - strokeWidth) / 2;
    const circumference = 2 * Math.PI * radius;
    const frequency = circumference / Math.max(0.1, wavelength);

    const SMOOTHING_FACTOR = 1; // Polynomial smoothing amplitude multiplier

    const drawActiveIndicator = (
      angleEnd: number,
      amplitudeCurrent: number,
    ) => {
      let activeIndicatorData = '';
      const resolution = 0.05;
      const endRad = angleEnd * 2 * Math.PI;

      for (let rad = 0; rad <= endRad; rad += resolution) {
        let currentRad = rad;
        // Ensure the last point connects exactly to the end
        if (rad > endRad - resolution && rad < endRad) {
          currentRad = endRad;
          rad = endRad; // To break loop after this iteration
        } else if (rad > endRad) {
          break;
        }

        // Height of the wave calculation at the current angle.
        const amplitudeOffset =
          amplitudeCurrent *
          Math.sin(frequency * currentRad + wavelengthOffset);

        // Wave smoothing factor calculation at the current angle.
        const power = Math.max(
          1 -
            (amplitudeCurrent - Math.abs(amplitudeOffset)) /
              Math.max(amplitudeCurrent, amplitude / 2),
          0,
        );
        const smoothingOffset =
          Math.sign(amplitudeOffset) *
          SMOOTHING_FACTOR *
          (amplitudeCurrent - Math.abs(amplitudeOffset)) *
          (power * power);

        // Calculate the point of the wave in x and y coordinates.
        const totalOffset = radius + amplitudeOffset + smoothingOffset;
        const x = center + totalOffset * Math.cos(currentRad);
        const y = center + totalOffset * Math.sin(currentRad);

        if (currentRad === 0) {
          activeIndicatorData += `M ${x} ${y} `;
        } else {
          activeIndicatorData += `L ${x} ${y} `;
        }
      }
      activeIndicator.setAttribute('d', activeIndicatorData);
    };

    const drawTrack = (angleStart: number, gapAngle: number) => {
      let trackData = '';
      const resolution = 0.05;
      const startRad = angleStart * 2 * Math.PI;
      const endRad = (1 - gapAngle) * 2 * Math.PI;

      for (let rad = startRad; rad <= endRad; rad += resolution) {
        let currentRad = rad;
        if (rad > endRad - resolution && rad < endRad) {
          currentRad = endRad;
          rad = endRad;
        } else if (rad > endRad) {
          break;
        }

        const x = center + radius * Math.cos(currentRad);
        const y = center + radius * Math.sin(currentRad);

        if (currentRad === startRad) {
          trackData += `M ${x} ${y} `;
        } else {
          trackData += `L ${x} ${y} `;
        }
      }
      track.setAttribute('d', trackData);
    };

    const targetAmplitude = isIndeterminate
      ? amplitude
      : progressValue >= 0.1 && progressValue < 0.95
        ? amplitude
        : 0;

    const drawWave = () => {
      const now = performance.now();
      const elapsedTime = Math.min(now - timeCurrent, 50); // Cap frame drops
      timeCurrent = now;

      if (wavespeed > 0) {
        wavelengthOffset =
          (wavelengthOffset + (2 * Math.PI * wavespeed * elapsedTime) / 1000) %
          (2 * Math.PI);
      }

      let animatingAmplitude = false;
      if (amplitudeRef.current !== targetAmplitude) {
        animatingAmplitude = true;
        const ANIMATION_DURATION_MS = 500;
        const diff = amplitude * (elapsedTime / ANIMATION_DURATION_MS);
        if (amplitudeRef.current < targetAmplitude) {
          amplitudeRef.current += diff;
          if (amplitudeRef.current > targetAmplitude) {
            amplitudeRef.current = targetAmplitude;
          }
        } else {
          amplitudeRef.current -= diff;
          if (amplitudeRef.current < targetAmplitude) {
            amplitudeRef.current = targetAmplitude;
          }
        }
      }

      drawActiveIndicator(1, amplitudeRef.current);
      drawTrack(0, 0);

      graphicEl.style.setProperty(
        '--active-circumference',
        `${Math.ceil(activeIndicator.getTotalLength())}px`,
      );
      graphicEl.style.setProperty(
        '--track-circumference',
        `${Math.ceil(track.getTotalLength())}px`,
      );

      if (wavespeed > 0 || animatingAmplitude) {
        animationFrameId = requestAnimationFrame(drawWave);
      }
    };

    drawWave();

    return () => {
      if (animationFrameId) cancelAnimationFrame(animationFrameId);
    };
  }, [
    isWavy,
    amplitude,
    wavelength,
    wavespeed,
    progressValue,
    isIndeterminate,
  ]);

  const wrapperProps = {
    className: `circular-progress-wrapper ${isWavy ? 'circular-progress-wavy-wrapper' : ''} ${className}`,
    'data-progressvalue': progressValue,
    'data-amplitude': isWavy ? amplitude : undefined,
    'data-wavelength': isWavy ? wavelength : undefined,
    'data-wavespeed': isWavy ? wavespeed : undefined,
    'aria-hidden': closed,
    style: {
      ...style,
      '--progress-value': progressValue,
    } as CSSProperties,
    ...rest,
  };

  const progressProps = {
    className: `circular-progress ${isWavy ? 'circular-progress-wavy' : ''}${stateClasses}`,
    role: 'progressbar',
    'aria-label': rest['aria-label'],
    'aria-valuemin': 0,
    'aria-valuemax': 1,
    'aria-valuenow': isIndeterminate ? undefined : progressValue,
  };

  return (
    <div {...wrapperProps}>
      <div {...progressProps} ref={graphicRef}>
        <div className="circular-progress-container">
          <svg
            className="circular-progress-circle-graphic"
            xmlns="http://www.w3.org/2000/svg">
            {isWavy ? (
              <>
                <path
                  ref={activeIndicatorRef}
                  className="circular-progress-active-indicator"
                  d=""
                />
                <path ref={trackRef} className="circular-progress-track" d="" />
              </>
            ) : (
              <>
                <circle ref={trackRef} className="circular-progress-track" />
                <circle
                  ref={activeIndicatorRef}
                  className="circular-progress-active-indicator"
                />
              </>
            )}
          </svg>
        </div>
      </div>
    </div>
  );
}
