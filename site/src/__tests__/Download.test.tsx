import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Download from '../components/Download';
import type { I18nMessages } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: '', tech: '', download: '' },
  hero: { title: '', subtitle: '', cta: '' },
  features: { heading: '', items: [] },
  tech: { heading: '' },
  screenshots: { heading: '' },
  download: {
    heading: 'Get the App',
    description: 'Build it yourself.',
    steps: ['Step A', 'Step B', 'Step C'],
  },
  footer: { built: '', license: '' },
  locale: { toggle: '' },
  theme: { light: '', dark: '', auto: '' },
};

describe('Download', () => {
  it('renders the heading', () => {
    render(<Download messages={mockMessages} />);
    expect(screen.getByText('Get the App')).toBeDefined();
  });

  it('renders the description', () => {
    render(<Download messages={mockMessages} />);
    expect(screen.getByText('Build it yourself.')).toBeDefined();
  });

  it('renders all steps as list items', () => {
    render(<Download messages={mockMessages} />);
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Step A');
    expect(items[1]).toHaveTextContent('Step B');
    expect(items[2]).toHaveTextContent('Step C');
  });

  it('renders step content', () => {
    render(<Download messages={mockMessages} />);
    expect(screen.getByText('Step A')).toBeDefined();
    expect(screen.getByText('Step B')).toBeDefined();
    expect(screen.getByText('Step C')).toBeDefined();
  });

  it('renders GitHub link', () => {
    render(<Download messages={mockMessages} />);
    const link = screen.getByText('GitHub');
    expect(link).toBeDefined();
    expect(link.closest('a')).toHaveAttribute('href', 'https://github.com/dekrate/KofeinoTracker');
  });
});
