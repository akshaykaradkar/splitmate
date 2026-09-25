/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import {WebComponentProps, createComponent} from '@lit/react';
import '@material/web/slider/slider.js';
import {MdSlider} from '@material/web/slider/slider.js';
import React from 'react';

declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      'md-slider': WebComponentProps<MdSlider>;
    }
  }
}

const SliderWebComponent = createComponent({
  tagName: 'md-slider',
  elementClass: MdSlider,
  react: React,
  events: {
    onchange: 'change',
    onChange: 'change',
    oninput: 'input',
    onInput: 'input',
  },
});

/** Props for the Slider component. */
export interface SliderProps
  extends React.ComponentProps<typeof SliderWebComponent> {}

/** Material 3 Slider component. */
export function Slider(props: SliderProps) {
  return <SliderWebComponent {...props} />;
}
