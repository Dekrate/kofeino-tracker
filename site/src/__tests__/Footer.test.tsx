import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Footer from '../components/Footer';
import type { I18nMessages } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: '', tech: '', download: '' },
  hero: { title: '', subtitle: '', cta: '' },
  features: { heading: '', items: [] },
  tech: { heading: '' },
  screenshots: { heading: '' },
  download: { heading: '', description: '', steps: [] },
  footer: { built: 'Built with love.', license: 'MIT' },
  locale: { toggle: '' },
  theme: { light: '', dark: '', auto: '' },
};

describe('Footer', () => {
  it('renders the built message', () => {
    render(<Footer messages={mockMessages} />);
    expect(screen.getByText('Built with love.')).toBeDefined();
  });

  it('renders the license text', () => {
    render(<Footer messages={mockMessages} />);
    expect(screen.getByText('MIT')).toBeDefined();
  });

  it('renders GitHub link', () => {
    render(<Footer messages={mockMessages} />);
    const link = screen.getByText('GitHub');
    expect(link).toBeDefined();
    expect(link.closest('a')).toHaveAttribute('href', 'https://github.com/dekrate/KofeinoTracker');
    expect(link.closest('a')).toHaveAttribute('target', '_blank');
    expect(link.closest('a')).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
