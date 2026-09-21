import '@testing-library/jest-dom/vitest';
// jsdom is missing support for some browser features like ElementInternals.
// Use element-internals-polyfill as a workaround and mock missing features.
import 'element-internals-polyfill';

if (typeof document !== 'undefined' && !document.adoptedStyleSheets) {
  Object.defineProperty(Document.prototype, 'adoptedStyleSheets', {
    value: [],
    writable: true,
  });
  Object.defineProperty(ShadowRoot.prototype, 'adoptedStyleSheets', {
    value: [],
    writable: true,
  });
}

// jsdom does not support CSS.registerProperty (CSS Houdini).
// The MWC nightly uses it for custom property registration.
if (typeof CSS !== 'undefined' && !CSS.registerProperty) {
  CSS.registerProperty = () => {};
}
