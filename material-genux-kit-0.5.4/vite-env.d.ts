/// <reference types="vite/client" />

// Type declaration for CSS imports with `{ type: 'css' }` assertion.
// These are handled by the cssSheetPlugin in vite.config.ts and resolve
// to native CSSStyleSheet objects at runtime.
declare module '*.css' {
  const sheet: CSSStyleSheet;
  export default sheet;
}
