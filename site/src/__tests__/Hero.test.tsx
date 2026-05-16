import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Hero from '../components/Hero';
import type { I18nMessages } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: '', tech: '', download: '' },
  hero: { title: 'KofeinoTracker', subtitle: 'Best tracker ever.', cta: 'Go!' },
  features: { heading: '', items: [] },
  tech: { heading: '' },
  screenshots: { heading: '' },
  download: { heading: '', description: '', steps: [] },
  footer: { built: '', license: '' },
  locale: { toggle: '' },
  theme: { light: '', dark: '', auto: '' },
};

describe('Hero', () => {
  it('renders title', () => {
    render(<Hero messages={mockMessages} />);
    expect(screen.getByText('KofeinoTracker')).toBeDefined();
  });

  it('renders subtitle', () => {
    render(<Hero messages={mockMessages} />);
    expect(screen.getByText('Best tracker ever.')).toBeDefined();
  });

  it('renders CTA button linking to download', () => {
    render(<Hero messages={mockMessages} />);
    const link = screen.getByText('Go!');
    expect(link).toBeDefined();
    expect(link.closest('a')).toHaveAttribute('href', '#download');
  });

  it('renders with a section wrapper', () => {
    const { container } = render(<Hero messages={mockMessages} />);
    const section = container.querySelector('section');
    expect(section).toBeDefined();
  });
});
