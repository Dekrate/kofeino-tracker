import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TechStack from '../components/TechStack';
import type { I18nMessages } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: '', tech: '', download: '' },
  hero: { title: '', subtitle: '', cta: '' },
  features: { heading: '', items: [] },
  tech: { heading: 'Stack' },
  screenshots: { heading: '' },
  download: { heading: '', description: '', steps: [] },
  footer: { built: '', license: '' },
  locale: { toggle: '' },
  theme: { light: '', dark: '', auto: '' },
};

describe('TechStack', () => {
  it('renders the heading', () => {
    render(<TechStack messages={mockMessages} />);
    expect(screen.getByText('Stack')).toBeDefined();
  });

  it('renders technology badges', () => {
    render(<TechStack messages={mockMessages} />);
    expect(screen.getByText('Kotlin')).toBeDefined();
    expect(screen.getByText('Jetpack Compose')).toBeDefined();
    expect(screen.getByText('Hilt DI')).toBeDefined();
    expect(screen.getByText('Room DB')).toBeDefined();
    expect(screen.getByText('Coroutines')).toBeDefined();
  });

  it('renders all technologies', () => {
    const { container } = render(<TechStack messages={mockMessages} />);
    const badges = container.querySelectorAll('span');
    expect(badges.length).toBeGreaterThanOrEqual(8);
  });
});
