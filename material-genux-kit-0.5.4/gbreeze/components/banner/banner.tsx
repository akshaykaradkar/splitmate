/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef, ReactNode} from 'react';
import './banner.css';

export interface BannerProps extends ComponentPropsWithRef<'div'> {
  graphic?: ReactNode;
  title?: string;
  text: string;
  actions?: ReactNode;
  closeIcon?: ReactNode;
  inset?: boolean;
  centered?: boolean;
  mobileStacked?: boolean;
}

export function Banner({
  graphic,
  title,
  text,
  actions,
  closeIcon,
  inset = false,
  centered = false,
  mobileStacked = false,
  className = '',
  ...props
}: BannerProps) {
  const isRich = title != null;
  const bannerClassName = [
    'banner',
    isRich ? 'banner-rich' : '',
    inset ? 'banner-inset' : '',
    centered ? 'banner-centered' : '',
    mobileStacked ? 'banner-mobile-stacked' : '',
    className ?? '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={bannerClassName} {...props}>
      <div className="banner-content-wrapper">
        <div className="banner-graphic-text">
          {graphic && <div className="banner-graphic">{graphic}</div>}
          <div className="banner-text-wrapper">
            {title && <div className="banner-title">{title}</div>}
            <div className="banner-text">{text}</div>
          </div>
        </div>
        {actions && <div className="banner-actions">{actions}</div>}
        {closeIcon && <div className="banner-close">{closeIcon}</div>}
      </div>
    </div>
  );
}
