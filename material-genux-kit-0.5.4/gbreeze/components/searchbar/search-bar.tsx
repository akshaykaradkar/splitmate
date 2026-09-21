/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef} from 'react';
import './search-bar.css';
import { Icon } from '../../styles/icons/icon';

export interface SearchBarProps extends ComponentPropsWithRef<'input'> {
  leadingIcon?: string;
  trailingIcon?: string;
  onTrailingIconClick?: () => void;
}

export function SearchBar({
  leadingIcon = 'search',
  trailingIcon,
  onTrailingIconClick,
  className,
  ref,
  ...props
}: SearchBarProps) {
  const classes = ['search-bar', className];

  return (
    <div className={classes.filter(Boolean).join(' ')}>
      <Icon className="search-bar-leading-icon">{leadingIcon}</Icon>
      <input ref={ref} type="text" className="search-bar-input" {...props} />
      {trailingIcon && (
        <button
          className="icon-btn icon-btn-standard icon-btn-sm ripple focus-ring-outer search-bar-trailing-icon"
          onClick={onTrailingIconClick}
          type="button"
        >
          <Icon>{trailingIcon}</Icon>
        </button>
      )}
    </div>
  );
}
