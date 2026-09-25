/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import React, {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';

/** Possible animation states for the energy surface. */
export type EnergyState =
  | 'idle'
  | 'anticipating'
  | 'receiving'
  | 'processing'
  | 'responding';

/** Energy color calculation algorithm mode. */
export type EnergyType = 'accents' | 'hue';

/**
 * Interface representing the md-gb-energy custom element instance.
 */
export interface EnergyElement extends HTMLElement {
  active: boolean;
  type: EnergyType;
  baseline: boolean;
  state: EnergyState;
  intensity: number;
  dynamicIntensity: number;
}

declare global {
  interface HTMLElementTagNameMap {
    'md-gb-energy': EnergyElement;
  }
}

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-gb-energy': React.DetailedHTMLProps<
        React.HTMLAttributes<EnergyElement>,
        EnergyElement
      > & {
        active?: boolean;
        type?: EnergyType;
        baseline?: boolean;
        state?: EnergyState;
        intensity?: number;
        dynamicintensity?: number;
        slot?: string;
      };
    }
  }
}

/** Props for the Energy React component wrapper. */
export interface EnergyProps
  extends Omit<React.HTMLAttributes<HTMLElement>, 'color'> {
  /** Whether the energy animation is active. */
  active?: boolean;
  /** The current animation state of the energy surface. */
  state?: EnergyState;
  /** The color calculation mode for the energy effect. */
  type?: EnergyType;
  /** Whether to use the baseline GM3 styling. */
  baseline?: boolean;
  /** Energy intensity (0 to 1). */
  intensity?: number;
  /** Dynamic intensity (for receiving and responding states). */
  dynamicIntensity?: number;
  /** Base color mapped to the `--color` CSS variable. */
  color?: string;
  /** Slot name for container projection (e.g. "container"). */
  slot?: string;
  /** Child nodes. */
  children?: React.ReactNode;
}

/** SCS URL for the self-registering md-gb-energy component bundle. */
export const SCS_ENERGY_URL =
  'https://static.corp.google.com/material-web/gbreeze/latest/components/energy/md-gb-energy.js';

const scriptPromises = new Map<string, Promise<void>>();

/**
 * Idempotently loads the md-gb-energy custom element script from SCS.
 * Deduplicates in-flight requests, checks DOM for existing scripts, and guards against SSR.
 */
export function loadEnergyScript(url: string = SCS_ENERGY_URL): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.resolve();
  }

  if (customElements.get('md-gb-energy')) {
    return Promise.resolve();
  }

  const existingPromise = scriptPromises.get(url);
  if (existingPromise) {
    return existingPromise;
  }

  const existingScript = document.querySelector<HTMLScriptElement>(
    `script[src="${url}"]`,
  );

  if (existingScript) {
    if (customElements.get('md-gb-energy')) {
      return Promise.resolve();
    }
    const promise = new Promise<void>((resolve, reject) => {
      let cleanup: () => void = () => {};

      const onLoad = () => {
        cleanup();
        customElements.whenDefined('md-gb-energy').then(
          () => resolve(),
          (err) => {
            scriptPromises.delete(url);
            reject(err);
          },
        );
      };

      const onError = (e: Event) => {
        cleanup();
        scriptPromises.delete(url);
        reject(e);
      };

      cleanup = () => {
        existingScript.removeEventListener('load', onLoad);
        existingScript.removeEventListener('error', onError);
      };

      existingScript.addEventListener('load', onLoad);
      existingScript.addEventListener('error', onError);

      if (customElements.get('md-gb-energy')) {
        cleanup();
        resolve();
      }
    });

    scriptPromises.set(url, promise);
    return promise;
  }

  const promise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = url;
    script.async = true;

    script.onload = () => {
      customElements.whenDefined('md-gb-energy').then(
        () => resolve(),
        (err) => {
          script.remove();
          scriptPromises.delete(url);
          reject(err);
        },
      );
    };

    script.onerror = (err) => {
      script.remove();
      scriptPromises.delete(url);
      reject(err);
    };

    document.head.appendChild(script);
  });

  scriptPromises.set(url, promise);
  return promise;
}

/**
 * Hook to track whether the md-gb-energy custom element has loaded.
 */
export function useEnergyLoaded(): boolean {
  const [loaded, setLoaded] = useState(() => {
    if (typeof window === 'undefined') return false;
    return !!customElements.get('md-gb-energy');
  });

  useEffect(() => {
    if (loaded) return;
    let isMounted = true;
    loadEnergyScript().then(
      () => {
        if (isMounted) {
          setLoaded(true);
        }
      },
      (err) => {
        console.error('Failed to load md-gb-energy script:', err);
      },
    );
    return () => {
      isMounted = false;
    };
  }, [loaded]);

  return loaded;
}

/**
 * React wrapper for md-gb-energy with dynamic SCS loading, DOM attribute
 * synchronization, ref forwarding, and slot support.
 */
export const Energy = forwardRef<EnergyElement, EnergyProps>(function Energy(
  {
    active,
    state,
    type,
    baseline,
    intensity,
    dynamicIntensity,
    color,
    slot,
    style,
    className,
    children,
    ...rest
  },
  ref,
) {
  const innerRef = useRef<EnergyElement>(null);
  useImperativeHandle(ref, () => innerRef.current as EnergyElement);

  const isLoaded = useEnergyLoaded();

  useEffect(() => {
    const el = innerRef.current;
    if (!el) return;

    if (active) {
      el.setAttribute('active', '');
    } else {
      el.removeAttribute('active');
    }

    if (baseline) {
      el.setAttribute('baseline', '');
    } else {
      el.removeAttribute('baseline');
    }

    if (state) {
      el.setAttribute('state', state);
    } else {
      el.removeAttribute('state');
    }

    if (type) {
      el.setAttribute('type', type);
    } else {
      el.removeAttribute('type');
    }

    if (intensity !== undefined) {
      el.setAttribute('intensity', String(intensity));
    } else {
      el.removeAttribute('intensity');
    }

    if (dynamicIntensity !== undefined) {
      el.setAttribute('dynamicintensity', String(dynamicIntensity));
    } else {
      el.removeAttribute('dynamicintensity');
    }
  }, [isLoaded, active, baseline, state, type, intensity, dynamicIntensity]);

  const combinedStyle: React.CSSProperties = {
    ...(slot === 'container'
      ? {
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          pointerEvents: 'none',
        }
      : undefined),
    ...(color ? ({'--color': color} as React.CSSProperties) : undefined),
    ...style,
  };

  return (
    <md-gb-energy
      ref={innerRef}
      active={active ? true : undefined}
      baseline={baseline ? true : undefined}
      state={state}
      type={type}
      intensity={intensity}
      dynamicintensity={dynamicIntensity}
      slot={slot}
      style={combinedStyle}
      className={className}
      {...rest}>
      {children}
    </md-gb-energy>
  );
});

Energy.displayName = 'Energy';
