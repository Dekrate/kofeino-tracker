import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Features from '../components/Features';
import type { I18nMessages } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: 'Features', tech: 'Tech', download: 'Get Started' },
  hero: { title: 'KofeinoTracker', subtitle: 'Tagline', cta: 'Go' },
  features: {
    heading: 'Built for your wrist',
    items: [
      { title: 'Feature One', description: 'Description one' },
      { title: 'Feature Two', description: 'Description two' },
    ],
  },
  tech: { heading: 'Tech' },
  screenshots: { heading: 'Screens' },
  download: { heading: 'Download', description: 'Desc', steps: ['Step 1'] },
  footer: { built: 'Built', license: 'MIT' },
  locale: { toggle: 'Switch' },
};

describe('Features', () => {
  it('renders the heading', () => {
    render(<Features messages={mockMessages} />);
    expect(screen.getByText('Built for your wrist')).toBeDefined();
  });

  it('renders all feature cards', () => {
    render(<Features messages={mockMessages} />);
    expect(screen.getByText('Feature One')).toBeDefined();
    expect(screen.getByText('Feature Two')).toBeDefined();
  });

  it('renders feature descriptions', () => {
    render(<Features messages={mockMessages} />);
    expect(screen.getByText('Description one')).toBeDefined();
    expect(screen.getByText('Description two')).toBeDefined();
  });

  it('renders correct number of features', () => {
    render(<Features messages={mockMessages} />);
    const articles = screen.getAllByRole('article');
    expect(articles).toHaveLength(2);
  });
});
