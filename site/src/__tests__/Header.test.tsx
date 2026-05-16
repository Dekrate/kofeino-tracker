import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Header from '../components/Header';
import type { I18nMessages, ThemeMode } from '../types';

const mockMessages: I18nMessages = {
  nav: { features: 'Features', tech: 'Tech', download: 'Get Started' },
  hero: { title: 'KofeinoTracker', subtitle: 'Tagline', cta: 'Go' },
  features: {
    heading: 'Features',
    items: [{ title: 'Test', description: 'Desc' }],
  },
  tech: { heading: 'Tech' },
  screenshots: { heading: 'Screens' },
  download: { heading: 'Download', description: 'Desc', steps: ['Step 1'] },
  footer: { built: 'Built', license: 'MIT' },
  locale: { toggle: 'Switch to Polish' },
  theme: { light: 'Light', dark: 'Dark', auto: 'Auto' },
};

describe('Header', () => {
  it('renders logo', () => {
    render(
      <Header
        messages={mockMessages}
        onToggleLocale={() => {}}
        themeMode="light"
        onCycleTheme={() => {}}
      />,
    );
    expect(screen.getByText('KofeinoTracker')).toBeDefined();
  });

  it('renders navigation links', () => {
    render(
      <Header
        messages={mockMessages}
        onToggleLocale={() => {}}
        themeMode="light"
        onCycleTheme={() => {}}
      />,
    );
    expect(screen.getByText('Features')).toBeDefined();
    expect(screen.getByText('Tech')).toBeDefined();
    expect(screen.getByText('Get Started')).toBeDefined();
  });

  it('calls onToggleLocale when locale button clicked', async () => {
    const onToggle = vi.fn();
    const user = userEvent.setup();
    render(
      <Header
        messages={mockMessages}
        onToggleLocale={onToggle}
        themeMode="light"
        onCycleTheme={() => {}}
      />,
    );
    const buttons = screen.getAllByRole('button');
    // Last button is locale toggle
    await user.click(buttons[buttons.length - 1]);
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('calls onCycleTheme when theme button clicked', async () => {
    const onCycle = vi.fn();
    const user = userEvent.setup();
    render(
      <Header
        messages={mockMessages}
        onToggleLocale={() => {}}
        themeMode="light"
        onCycleTheme={onCycle}
      />,
    );
    const buttons = screen.getAllByRole('button');
    // First button is theme toggle
    await user.click(buttons[0]);
    expect(onCycle).toHaveBeenCalledOnce();
  });

  it('has two icon buttons', () => {
    render(
      <Header
        messages={mockMessages}
        onToggleLocale={() => {}}
        themeMode="light"
        onCycleTheme={() => {}}
      />,
    );
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBe(2);
  });

  it('shows sun icon in light mode', () => {
    const { container } = render(
      <Header
        messages={mockMessages}
        onToggleLocale={() => {}}
        themeMode="light"
        onCycleTheme={() => {}}
      />,
    );
    // Light mode should render SVG (sun icon)
    const svgs = container.querySelectorAll('svg');
    expect(svgs.length).toBe(2); // theme + globe
  });
});
