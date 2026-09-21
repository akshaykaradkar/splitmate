/**
 * Copyright 2026 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
/** Note: AI-generated */
import {ComponentPropsWithRef} from 'react';
import './carousel.css';

export interface CarouselItem {
  id: string | number;
  image: string;
  title?: string;
  subtitle?: string;
}

export interface CarouselProps extends ComponentPropsWithRef<'div'> {
  items: CarouselItem[];
  variant?: 'multi-browse' | 'hero' | 'full-screen';
}

export function Carousel({
  items,
  variant = 'multi-browse',
  className = '',
  ...props
}: CarouselProps) {
  if (variant === 'full-screen') {
    return (
      <div
        className={`carousel carousel-full-screen ${className ?? ''}`}
        {...props}>
        <div className="carousel-scroller">
          {items.map((item) => (
            <div key={item.id} className="carousel-item">
              <div className="carousel-item-media">
                <img src={item.image} alt={item.title || ''} />
              </div>
              {(item.title || item.subtitle) && (
                <div className="carousel-scrim">
                  {item.title && (
                    <h3 className="carousel-item-title">{item.title}</h3>
                  )}
                  {item.subtitle && (
                    <p className="carousel-item-subtitle">{item.subtitle}</p>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (variant === 'hero') {
    return (
      <div className={`carousel carousel-hero ${className ?? ''}`} {...props}>
        <div className="carousel-scroller">
          {items.map((item) => (
            <div key={item.id} className="carousel-item">
              <div className="carousel-item-media">
                <img src={item.image} alt={item.title || ''} />
              </div>
              {(item.title || item.subtitle) && (
                <div className="carousel-scrim">
                  {item.title && (
                    <h3 className="carousel-item-title">{item.title}</h3>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Multi-browse (default)
  return (
    <div
      className={`carousel carousel-multi-browse ${className ?? ''}`}
      {...props}>
      <div className="carousel-scroller">
        {items.map((item) => (
          <div key={item.id} className="carousel-item">
            <div className="carousel-item-media">
              <img src={item.image} alt={item.title || ''} />
            </div>
            {item.title && (
              <span className="carousel-item-title">{item.title}</span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
