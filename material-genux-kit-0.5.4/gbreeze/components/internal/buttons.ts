/**
 * @license
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import { isServer, LitElement } from 'lit';
import { property } from 'lit/decorators.js';

import { mixinDelegatesAria } from '@material/web/internal/aria/delegate';
import {
  afterDispatch,
  setupDispatchHooks,
} from '@material/web/internal/events/dispatch-hooks';
import { mixinElementInternals } from '@material/web/labs/behaviors/element-internals';
import { mixinFormAssociated } from '@material/web/labs/behaviors/form-associated';
import { mixinFormSubmitter } from '@material/web/labs/behaviors/form-submitter';

// Separate variable needed for closure.
const delegatesBaseClass = mixinDelegatesAria(
  mixinFormSubmitter(mixinFormAssociated(mixinElementInternals(LitElement))),
);

/**
 * Base class for components that delegate focus to an internal `<button>`.
 */
export class DelegatesButton extends delegatesBaseClass {
  /** @nocollapse */
  static override shadowRootOptions: ShadowRootInit = {
    mode: 'open',
    delegatesFocus: true,
  };

  // TODO: consider adding formId property for React to form-associated mixin
  get formId() {
    return this.getAttribute('form') ?? '';
  }
  set formId(formId: string) {
    this.setAttribute('form', formId);
  }

  /**
   * Whether or not the button is "soft-disabled" (disabled but still
   * focusable).
   *
   * Use this when a button needs increased visibility when disabled. See
   * https://www.w3.org/WAI/ARIA/apg/practices/keyboard-interface/#kbd_disabled_controls
   * for more guidance on when this is needed.
   */
  @property({ type: Boolean, attribute: 'soft-disabled', reflect: true })
  softDisabled = false;

  constructor() {
    super();
    if (isServer) return;
    this.addEventListener('click', (event) => {
      // If the button is soft-disabled, we need to explicitly prevent the click
      // from propagating to other event listeners as well as prevent the
      // default action. This is because the underlying `<button>` element is
      // not actually `:disabled`.
      if (this.softDisabled) {
        event.stopImmediatePropagation();
        event.preventDefault();
        return;
      }
    });
  }
}

/**
 * Base class for components that delegate focus to a `<button>` element in
 * their shadow dom. Supports `type="toggle"` button behavior.
 */
export class DelegatesToggleableButton extends DelegatesButton {
  /**
   * Whether or not the button is selected, when `type="toggle"`.
   */
  @property({ type: Boolean }) selected = false;

  constructor() {
    super();
    if (isServer) return;
    setupDispatchHooks(this, 'click');
    this.addEventListener('click', (event) => {
      afterDispatch(event, () => {
        if (
          event.defaultPrevented ||
          this.type !== 'toggle' ||
          this.disabled ||
          this.softDisabled
        ) {
          return;
        }

        this.selected = !this.selected;
        // Mimic native browser toggle behavior events.
        this.dispatchEvent(
          new InputEvent('input', { bubbles: true, composed: true }),
        );
        this.dispatchEvent(new Event('change', { bubbles: true }));
      });
    });
  }
}

/**
 * Base class for components that delegate focus to a `<button>` or `<a>`
 * element in their shadow dom. Supports `type="toggle"` and `type="link"`
 * button behaviors.
 */
export class DelegatesToggleableButtonOrLink extends DelegatesToggleableButton {
  /**
   * A string indicating the behavior of the button.
   *
   * - "submit" (default): A button that submits its associated form.
   * - "reset": A button that resets its associated form.
   * - "button": A normal button.
   * - "toggle": A toggle button using the `selected` property.
   * - "link": An anchor link (`<a>`). Type is always "link" when `href` is set.
   */
  @property({ noAccessor: true })
  override get type(): string {
    return this.href ? 'link' : super.type;
  }
  override set type(type: string) {
    if (this.href && type !== 'link') {
      return;
    }
    super.type = type;
  }

  /**
   * The URL that the link button points to.
   */
  @property() href = '';

  /**
   * The filename to use when downloading the linked resource.
   * If not specified, the browser will determine a filename.
   * This is only applicable when the button is used as a link (`href` is set).
   */
  @property() download = '';

  /**
   * Where to display the linked `href` URL for a link button. Common options
   * include `_blank` to open in a new tab.
   */
  @property() target: '_blank' | '_parent' | '_self' | '_top' | '' = '';

  constructor() {
    super();
    if (isServer) return;
    this.addEventListener('click', (event) => {
      // If the button is a disabled link, we need to explicitly prevent the
      // click from propagating to other event listeners as well as prevent
      // the default action. This is because the underlying `<a>` element is
      // not actually disabled.
      if (this.href && (this.disabled || this.softDisabled)) {
        event.stopImmediatePropagation();
        event.preventDefault();
        return;
      }
    });
  }
}
