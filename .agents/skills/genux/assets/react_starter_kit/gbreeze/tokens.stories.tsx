/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

// Storybook requires default exports
// tslint:disable:no-default-export

import {PrimaryTab, Tabs} from '@/gbreeze/components/tabs/tabs';
import {useState} from 'react';
import preview from '../storybook/preview';

const meta = preview.meta({
  title: 'Design Tokens/Reference',
});

/** Storybook meta */
export default meta;

const ColorCard = ({
  bgClass,
  textClass,
  contentClasses,
}: {
  bgClass: string;
  textClass: string;
  contentClasses?: string[];
}) => {
  const isDark =
    bgClass === 'bg-scrim' ||
    bgClass === 'bg-shadow' ||
    bgClass === 'bg-inverse-surface';
  const isWhite =
    bgClass === 'bg-surface' ||
    bgClass === 'bg-surface-bright' ||
    bgClass === 'bg-surface-container-lowest';
  const borderClass = isWhite
    ? 'border-outline-variant'
    : 'border-outline-variant/10';
  return (
    <div
      style={{color: textClass === 'text-white' ? 'white' : undefined}}
      className={`${bgClass} ${textClass} p-2 rounded-lg flex flex-col gap-2 min-h-[120px] border ${borderClass}`}>
      <div>
        <span
          style={{opacity: 0.6}}
          className="text-[10px] font-medium uppercase tracking-wider">
          BACKGROUND
        </span>
        <div
          className={`typescale-body-sm font-mono font-medium mt-1 ${isDark ? 'text-white' : ''}`}>
          {bgClass}
        </div>
      </div>
      <div>
        <span
          style={{opacity: 0.6}}
          className="text-[10px] font-medium uppercase tracking-wider">
          CONTENT
        </span>
        <div className="flex flex-col gap-0.5 mt-0.5">
          {contentClasses ? (
            contentClasses.map((c) => (
              <div key={c} className={`typescale-label-sm font-mono ${c}`}>
                {c}
              </div>
            ))
          ) : (
            <div className="typescale-label-sm font-mono">{textClass}</div>
          )}
        </div>
      </div>
    </div>
  );
};

const VariableColorCard = ({
  bgVar,
  textVar,
  contentVars,
}: {
  bgVar: string;
  textVar: string;
  contentVars?: string[];
}) => {
  const isDark =
    bgVar === '--md-sys-color-scrim' ||
    bgVar === '--md-sys-color-shadow' ||
    bgVar === '--md-sys-color-inverse-surface';
  const isWhite =
    bgVar === '--md-sys-color-surface' ||
    bgVar === '--md-sys-color-surface-bright' ||
    bgVar === '--md-sys-color-surface-container-lowest';
  const borderClass = isWhite
    ? 'border-outline-variant'
    : 'border-outline-variant/10';
  return (
    <div
      style={{
        backgroundColor: `var(${bgVar})`,
        color: textVar.startsWith('--') ? `var(${textVar})` : textVar,
      }}
      className={`p-2 rounded-lg flex flex-col gap-2 min-h-[120px] border ${borderClass}`}>
      <div>
        <span
          style={{opacity: 0.6}}
          className="text-[10px] font-medium uppercase tracking-wider">
          BACKGROUND
        </span>
        <div
          className={`typescale-body-sm font-mono font-medium mt-0.5 ${isDark ? 'text-white' : ''}`}>
          {bgVar}
        </div>
      </div>
      <div>
        <span
          style={{opacity: 0.6}}
          className="text-[10px] font-medium uppercase tracking-wider">
          CONTENT
        </span>
        <div className="flex flex-col gap-0.5 mt-0.5">
          {contentVars ? (
            contentVars.map((c) => (
              <div
                key={c}
                style={{color: c.startsWith('--') ? `var(${c})` : c}}
                className="typescale-label-sm font-mono">
                {c}
              </div>
            ))
          ) : (
            <div
              className={`typescale-label-sm font-mono ${isDark ? 'text-white' : ''}`}>
              {textVar}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/** ClassNames story */
export const ClassNames = meta.story({
  render: () => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const [activeTab, setActiveTab] = useState(0);

    return (
      <div className="bg-surface">
        <h1 className="typescale-headline-lg mb-2 text-on-surface">
          Design Tokens as Class Names
        </h1>
        <p className="typescale-body-lg mb-2 text-on-surface-variant">
          Reference for Tailwind classes derived from Material Design 3 tokens
          in gBreeze. By default, you can simply use these CSS class names for
          standard styling.
        </p>

        <div className="mb-4 p-2 bg-surface-container-low rounded-lg max-w-4xl">
          <p className="typescale-body-md text-on-surface mb-1">
            Usage Examples:
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-on-surface-variant typescale-body-sm">
            <div>
              <code className="block p-1 bg-surface-container rounded font-mono text-xs overflow-x-auto">
                {'<div className="bg-primary text-on-primary">...</div>'}
              </code>
            </div>
          </div>
        </div>

        <Tabs
          activeTabIndex={activeTab}
          onchange={(e: any) => setActiveTab(e.target.activeTabIndex)}
          className="mb-3">
          <PrimaryTab>Colors</PrimaryTab>
          <PrimaryTab>Typography</PrimaryTab>
          <PrimaryTab>Elevation</PrimaryTab>
          <PrimaryTab>Shape</PrimaryTab>
          <PrimaryTab>Spacing</PrimaryTab>
        </Tabs>

        <div className="mt-3">
          {activeTab === 0 && (
            <section className="mb-6">
              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Core Palette
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <ColorCard bgClass="bg-primary" textClass="text-on-primary" />
                <ColorCard
                  bgClass="bg-primary-container"
                  textClass="text-on-primary-container"
                />
                <ColorCard
                  bgClass="bg-secondary"
                  textClass="text-on-secondary"
                />
                <ColorCard
                  bgClass="bg-secondary-container"
                  textClass="text-on-secondary-container"
                />

                <ColorCard bgClass="bg-tertiary" textClass="text-on-tertiary" />
                <ColorCard
                  bgClass="bg-tertiary-container"
                  textClass="text-on-tertiary-container"
                />
                <ColorCard bgClass="bg-error" textClass="text-on-error" />
                <ColorCard
                  bgClass="bg-error-container"
                  textClass="text-on-error-container"
                />
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Surface Colors
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <ColorCard
                  bgClass="bg-surface"
                  textClass="text-on-surface"
                  contentClasses={[
                    'text-on-surface',
                    'text-on-surface-variant',
                    'text-primary',
                    'text-secondary',
                    'text-tertiary',
                    'text-error',
                  ]}
                />
                <ColorCard
                  bgClass="bg-surface-bright"
                  textClass="text-on-surface"
                />
                <ColorCard
                  bgClass="bg-surface-dim"
                  textClass="text-on-surface"
                />
                <ColorCard
                  bgClass="bg-surface-container-lowest"
                  textClass="text-on-surface"
                />

                <ColorCard
                  bgClass="bg-surface-container-low"
                  textClass="text-on-surface"
                />
                <ColorCard
                  bgClass="bg-surface-container"
                  textClass="text-on-surface"
                />
                <ColorCard
                  bgClass="bg-surface-container-high"
                  textClass="text-on-surface"
                />
                <ColorCard
                  bgClass="bg-surface-container-highest"
                  textClass="text-on-surface"
                />
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Fixed Colors
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <ColorCard
                  bgClass="bg-primary-fixed"
                  textClass="text-on-primary-fixed"
                />
                <ColorCard
                  bgClass="bg-primary-fixed-dim"
                  textClass="text-on-primary-fixed"
                />
                <ColorCard
                  bgClass="bg-secondary-fixed"
                  textClass="text-on-secondary-fixed"
                />
                <ColorCard
                  bgClass="bg-secondary-fixed-dim"
                  textClass="text-on-secondary-fixed"
                />
                <ColorCard
                  bgClass="bg-tertiary-fixed"
                  textClass="text-on-tertiary-fixed"
                />
                <ColorCard
                  bgClass="bg-tertiary-fixed-dim"
                  textClass="text-on-tertiary-fixed"
                />
                <div className="col-span-2"></div> {/* Spacer */}
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Inverse & Others
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                <ColorCard
                  bgClass="bg-inverse-surface"
                  textClass="text-inverse-on-surface"
                  contentClasses={[
                    'text-inverse-on-surface',
                    'text-inverse-primary',
                  ]}
                />

                <div className="p-2 rounded-lg border border-outline flex flex-col justify-between min-h-[120px] bg-surface">
                  <div>
                    <span
                      style={{opacity: 0.6}}
                      className="text-[10px] font-medium uppercase tracking-wider text-on-surface">
                      BORDER
                    </span>
                    <div className="typescale-body-sm font-mono font-medium mt-0.5 text-on-surface">
                      border-outline
                    </div>
                  </div>
                </div>

                <div className="p-2 rounded-lg border border-outline-variant flex flex-col justify-between min-h-[120px] bg-surface">
                  <div>
                    <span
                      style={{opacity: 0.6}}
                      className="text-[10px] font-medium uppercase tracking-wider text-on-surface">
                      BORDER
                    </span>
                    <div className="typescale-body-sm font-mono font-medium mt-0.5 text-on-surface">
                      border-outline-variant
                    </div>
                  </div>
                </div>

                <div className="flex flex-col gap-1">
                  <ColorCard bgClass="bg-scrim" textClass="text-white" />
                  <ColorCard bgClass="bg-shadow" textClass="text-white" />
                </div>
              </div>
            </section>
          )}
          {activeTab === 1 && (
            <section className="mb-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {[
                  'typescale-display-lg',
                  'typescale-display-md',
                  'typescale-display-sm',
                  'typescale-headline-lg',
                  'typescale-headline-md',
                  'typescale-headline-sm',
                  'typescale-title-lg',
                  'typescale-title-md',
                  'typescale-title-sm',
                  'typescale-body-lg',
                  'typescale-body-md',
                  'typescale-body-sm',
                  'typescale-label-lg',
                  'typescale-label-md',
                  'typescale-label-sm',
                ].map((cls) => (
                  <div
                    key={cls}
                    className="p-2 rounded-lg bg-surface-container flex flex-col justify-between min-h-[120px]">
                    <p className={`${cls} text-on-surface`}>
                      {cls
                        .replace('typescale-', '')
                        .split('-')
                        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
                        .join(' ')}
                    </p>
                    <code className="text-sm text-on-surface-variant font-mono">
                      {cls}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 2 && (
            <section className="mb-6">
              <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
                {[
                  {size: 'xs', class: 'shadow-xs'},
                  {size: 'sm', class: 'shadow-sm'},
                  {size: 'md', class: 'shadow-md'},
                  {size: 'lg', class: 'shadow-lg'},
                  {size: 'xl', class: 'shadow-xl'},
                ].map((item) => (
                  <div
                    key={item.size}
                    className={`p-2 rounded-lg ${item.class} bg-surface-container`}>
                    <p className="text-on-surface">
                      Shadow {item.size.toUpperCase()}
                    </p>
                    <code className="text-sm text-on-surface-variant">
                      {item.class}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 3 && (
            <section className="mb-6">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                {[
                  {size: 'none', class: 'rounded-none'},
                  {size: 'xs', class: 'rounded-xs'},
                  {size: 'sm', class: 'rounded-sm'},
                  {size: 'md', class: 'rounded-md'},
                  {size: 'lg', class: 'rounded-lg'},
                  {size: 'xl', class: 'rounded-xl'},
                  {size: '2xl', class: 'rounded-2xl'},
                  {size: '3xl', class: 'rounded-3xl'},
                  {size: '4xl', class: 'rounded-4xl'},
                  {size: 'full', class: 'rounded-full'},
                ].map((item) => (
                  <div
                    key={item.size}
                    className={`p-2 ${item.class} bg-surface-container`}>
                    <p className="text-on-surface">
                      Radius {item.size.toUpperCase()}
                    </p>
                    <code className="text-sm text-on-surface-variant">
                      {item.class}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 4 && (
            <section className="mb-6">
              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Spacing Utilities
              </h3>
              <p className="typescale-body-md mb-4 text-on-surface-variant">
                Tailwind spacing utilities (padding, margin, gap) are half the
                size of the gBreeze spacing unit (default: 8px). For example,{' '}
                <code>p-1</code> is 4px, <code>p-2</code> is 8px, etc.
              </p>
              <p className="typescale-body-md mb-4 text-on-surface-variant">
                Alternatively, Material spacing units can be used. For example,{' '}
                <code>.p-s50</code> is 4px, <code>.p-s100</code> is 8px, etc.
              </p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {[
                  {value: 0, class: 'p-0', materialClass: 'p-s0'},
                  {value: 2, class: 'p-0.5', materialClass: 'p-s25'},
                  {value: 4, class: 'p-1', materialClass: 'p-s50'},
                  {value: 6, class: 'p-1.5', materialClass: 'p-s75'},
                  {value: 8, class: 'p-2', materialClass: 'p-s100'},
                  {value: 10, class: 'p-2.5', materialClass: 'p-s125'},
                  {value: 12, class: 'p-3', materialClass: 'p-s150'},
                  {value: 14, class: 'p-3.5', materialClass: 'p-s175'},
                  {value: 16, class: 'p-4', materialClass: 'p-s200'},
                  {value: 20, class: 'p-5', materialClass: 'p-s250'},
                  {value: 24, class: 'p-6', materialClass: 'p-s300'},
                  {value: 32, class: 'p-8', materialClass: 'p-s400'},
                  {value: 36, class: 'p-9', materialClass: 'p-s450'},
                  {value: 40, class: 'p-10', materialClass: 'p-s500'},
                  {value: 48, class: 'p-12', materialClass: 'p-s600'},
                  {value: 56, class: 'p-14', materialClass: 'p-s700'},
                  {value: 64, class: 'p-16', materialClass: 'p-s800'},
                  {value: 72, class: 'p-18', materialClass: 'p-s900'},
                ].map((item) => (
                  <div
                    key={item.class}
                    className="p-2 rounded-lg bg-surface-container flex justify-between items-center">
                    <div className="flex flex-col gap-0.5">
                      <span className="text-on-surface font-medium">
                        {item.value}px
                      </span>
                      <code className="text-xs text-on-surface-variant font-mono">
                        {item.class} | {item.materialClass}
                      </code>
                    </div>
                    <div
                      className="bg-on-surface-variant rounded"
                      style={{height: '24px', width: `${item.value}px`}}></div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      </div>
    );
  },
});

/** CssVariables story */
export const CssVariables = meta.story({
  name: 'CSS Variables',
  render: () => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const [activeTab, setActiveTab] = useState(0);

    return (
      <div className="bg-surface">
        <h1 className="typescale-headline-lg mb-2 text-on-surface">
          Design Tokens as CSS Variables
        </h1>
        <p className="typescale-body-lg mb-2 text-on-surface-variant">
          Reference for CSS variables derived from Material Design 3 tokens in
          gBreeze.
        </p>

        <div className="mb-3 p-2 bg-surface-container-low rounded-lg max-w-4xl">
          <p className="typescale-body-md text-on-surface">
            When to use CSS Variables: CSS variables are useful when you are
            writing custom CSS (e.g., in a `.css` file), using inline styles
            (via the React <code>style</code> attribute), or when a property
            cannot be easily styled with standard Tailwind classes (such as
            complex box-shadows or dynamic colors).
          </p>
        </div>

        <div className="mb-4 p-2 bg-surface-container-low rounded-lg max-w-4xl">
          <p className="typescale-body-md text-on-surface mb-1">
            Usage Examples:
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-on-surface-variant typescale-body-sm">
            <div>
              <p className="font-medium text-on-surface mb-0.5">
                System design tokens in a custom CSS class
              </p>
              <code className="block p-1 bg-surface-container rounded font-mono text-xs overflow-x-auto">
                {'.my-class { color: var(--md-sys-color-primary); }'}
              </code>
            </div>
            <div>
              <p className="font-medium text-on-surface mb-0.5">
                Reference design tokens for light and dark modes
              </p>
              <pre className="block p-1 bg-surface-container rounded font-mono text-xs whitespace-pre overflow-x-auto">
                {`.my-class {
  background-color: light-dark(var(--md-ref-palette-green90), var(--md-ref-palette-green30));
  color: light-dark(var(--md-ref-palette-green10), var(--md-ref-palette-green95));
}`}
              </pre>
            </div>
          </div>
        </div>

        <Tabs
          activeTabIndex={activeTab}
          onchange={(e: any) => setActiveTab(e.target.activeTabIndex)}
          className="mb-3">
          <PrimaryTab>Colors</PrimaryTab>
          <PrimaryTab>Typography</PrimaryTab>
          <PrimaryTab>Elevation</PrimaryTab>
          <PrimaryTab>Shape</PrimaryTab>
          <PrimaryTab>Spacing</PrimaryTab>
        </Tabs>

        <div className="mt-3">
          {activeTab === 0 && (
            <section className="mb-6">
              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Core Palette
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <VariableColorCard
                  bgVar="--md-sys-color-primary"
                  textVar="--md-sys-color-on-primary"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-primary-container"
                  textVar="--md-sys-color-on-primary-container"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-secondary"
                  textVar="--md-sys-color-on-secondary"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-secondary-container"
                  textVar="--md-sys-color-on-secondary-container"
                />

                <VariableColorCard
                  bgVar="--md-sys-color-tertiary"
                  textVar="--md-sys-color-on-tertiary"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-tertiary-container"
                  textVar="--md-sys-color-on-tertiary-container"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-error"
                  textVar="--md-sys-color-on-error"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-error-container"
                  textVar="--md-sys-color-on-error-container"
                />
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Surface Colors
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <VariableColorCard
                  bgVar="--md-sys-color-surface"
                  textVar="--md-sys-color-on-surface"
                  contentVars={[
                    '--md-sys-color-on-surface',
                    '--md-sys-color-on-surface-variant',
                    '--md-sys-color-primary',
                    '--md-sys-color-secondary',
                    '--md-sys-color-tertiary',
                    '--md-sys-color-error',
                  ]}
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-bright"
                  textVar="--md-sys-color-on-surface"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-dim"
                  textVar="--md-sys-color-on-surface"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-container-lowest"
                  textVar="--md-sys-color-on-surface"
                />

                <VariableColorCard
                  bgVar="--md-sys-color-surface-container-low"
                  textVar="--md-sys-color-on-surface"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-container"
                  textVar="--md-sys-color-on-surface"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-container-high"
                  textVar="--md-sys-color-on-surface"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-surface-container-highest"
                  textVar="--md-sys-color-on-surface"
                />
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Fixed Colors
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                <VariableColorCard
                  bgVar="--md-sys-color-primary-fixed"
                  textVar="--md-sys-color-on-primary-fixed"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-primary-fixed-dim"
                  textVar="--md-sys-color-on-primary-fixed"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-secondary-fixed"
                  textVar="--md-sys-color-on-secondary-fixed"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-secondary-fixed-dim"
                  textVar="--md-sys-color-on-secondary-fixed"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-tertiary-fixed"
                  textVar="--md-sys-color-on-tertiary-fixed"
                />
                <VariableColorCard
                  bgVar="--md-sys-color-tertiary-fixed-dim"
                  textVar="--md-sys-color-on-tertiary-fixed"
                />
                <div className="col-span-2"></div> {/* Spacer */}
              </div>

              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Inverse & Others
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                <VariableColorCard
                  bgVar="--md-sys-color-inverse-surface"
                  textVar="--md-sys-color-inverse-on-surface"
                  contentVars={[
                    '--md-sys-color-inverse-on-surface',
                    '--md-sys-color-inverse-primary',
                  ]}
                />

                <div
                  style={{borderColor: 'var(--md-sys-color-outline)'}}
                  className="p-2 rounded-lg border flex flex-col justify-between min-h-[120px] bg-surface">
                  <div>
                    <span
                      style={{opacity: 0.6}}
                      className="text-[10px] font-medium uppercase tracking-wider text-on-surface">
                      BORDER
                    </span>
                    <div className="typescale-body-sm font-mono font-medium mt-0.5 text-on-surface">
                      --md-sys-color-outline
                    </div>
                  </div>
                </div>

                <div
                  style={{borderColor: 'var(--md-sys-color-outline-variant)'}}
                  className="p-2 rounded-lg border flex flex-col justify-between min-h-[120px] bg-surface">
                  <div>
                    <span
                      style={{opacity: 0.6}}
                      className="text-[10px] font-medium uppercase tracking-wider text-on-surface">
                      BORDER
                    </span>
                    <div className="typescale-body-sm font-mono font-medium mt-0.5 text-on-surface">
                      --md-sys-color-outline-variant
                    </div>
                  </div>
                </div>

                <div className="flex flex-col gap-1">
                  <VariableColorCard
                    bgVar="--md-sys-color-scrim"
                    textVar="white"
                  />
                  <VariableColorCard
                    bgVar="--md-sys-color-shadow"
                    textVar="white"
                  />
                </div>
              </div>
            </section>
          )}
          {activeTab === 1 && (
            <section className="mb-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {[
                  'display-lg',
                  'display-md',
                  'display-sm',
                  'headline-lg',
                  'headline-md',
                  'headline-sm',
                  'title-lg',
                  'title-md',
                  'title-sm',
                  'body-lg',
                  'body-md',
                  'body-sm',
                  'label-lg',
                  'label-md',
                  'label-sm',
                ].map((size) => (
                  <div
                    key={size}
                    className="p-2 rounded-lg bg-surface-container flex flex-col justify-between min-h-[120px]">
                    <p
                      style={{
                        font: `var(--md-sys-typescale-${size})`,
                        letterSpacing: `var(--md-sys-typescale-${size}-tracking)`,
                      }}
                      className="text-on-surface">
                      {size
                        .split('-')
                        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
                        .join(' ')}
                    </p>
                    <code className="text-sm text-on-surface-variant font-mono">
                      --md-sys-typescale-{size}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 2 && (
            <section className="mb-6">
              <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
                {['1', '2', '3', '4', '5'].map((size) => (
                  <div
                    key={size}
                    style={{
                      boxShadow: `var(--md-sys-elevation-shadow-${size})`,
                    }}
                    className="p-2 rounded-lg bg-surface-container">
                    <p className="text-on-surface">Shadow {size}</p>
                    <code className="text-sm text-on-surface-variant">
                      --md-sys-elevation-shadow-{size}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 3 && (
            <section className="mb-6">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                {[
                  'none',
                  'xs',
                  'sm',
                  'md',
                  'lg',
                  'lg-increased',
                  'xl',
                  'xl-increased',
                  'xxl',
                  'full',
                ].map((size) => (
                  <div
                    key={size}
                    style={{
                      borderRadius: `var(--md-sys-shape-corner-${size})`,
                    }}
                    className="p-2 bg-surface-container">
                    <p className="text-on-surface">
                      Radius {size.toUpperCase()}
                    </p>
                    <code className="text-sm text-on-surface-variant">
                      --md-sys-shape-corner-{size}
                    </code>
                  </div>
                ))}
              </div>
            </section>
          )}
          {activeTab === 4 && (
            <section className="mb-6">
              <h3 className="typescale-title-lg mb-2 text-on-surface">
                Spacing Variables
              </h3>
              <p className="typescale-body-md mb-4 text-on-surface-variant">
                Use these CSS variables for custom spacing in your components.
              </p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {[
                  {var: '--md-sys-space-unit', val: '8px (Base unit)'},
                  {var: '--md-sys-space-0', val: '0px'},
                  {var: '--md-sys-space-25', val: '2px'},
                  {var: '--md-sys-space-50', val: '4px'},
                  {var: '--md-sys-space-75', val: '6px'},
                  {var: '--md-sys-space-100', val: '8px'},
                  {var: '--md-sys-space-125', val: '10px'},
                  {var: '--md-sys-space-150', val: '12px'},
                  {var: '--md-sys-space-175', val: '14px'},
                  {var: '--md-sys-space-200', val: '16px'},
                  {var: '--md-sys-space-250', val: '20px'},
                  {var: '--md-sys-space-300', val: '24px'},
                  {var: '--md-sys-space-400', val: '32px'},
                  {var: '--md-sys-space-450', val: '36px'},
                  {var: '--md-sys-space-500', val: '40px'},
                  {var: '--md-sys-space-600', val: '48px'},
                  {var: '--md-sys-space-700', val: '56px'},
                  {var: '--md-sys-space-800', val: '64px'},
                  {var: '--md-sys-space-900', val: '72px'},
                ].map((item) => (
                  <div
                    key={item.var}
                    className="p-2 rounded-lg bg-surface-container flex justify-between items-center">
                    <div className="flex flex-col gap-0.5">
                      <p className="text-on-surface font-medium">{item.val}</p>
                      <code className="text-xs text-on-surface-variant font-mono">
                        {item.var}
                      </code>
                    </div>
                    <div
                      className="bg-on-surface-variant rounded"
                      style={{
                        height: '24px',
                        width: item.val.includes('px')
                          ? item.val.split(' ')[0]
                          : '0px',
                      }}></div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      </div>
    );
  },
});
