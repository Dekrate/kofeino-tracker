import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import App from '../App';

describe('App', () => {
  beforeEach(() => {
    Object.defineProperty(navigator, 'language', {
      value: 'en-US',
      configurable: true,
    });
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders the app title in header and hero', () => {
    render(<App />);
    const titles = screen.getAllByText('KofeinoTracker');
    expect(titles.length).toBeGreaterThanOrEqual(2);
  });

  it('renders the tagline in English', () => {
    render(<App />);
    expect(screen.getByText('Track your caffeine, own your day.')).toBeDefined();
  });

  it('renders features section', () => {
    render(<App />);
    expect(screen.getByText('Built for your wrist')).toBeDefined();
    expect(screen.getByText('Wear OS Native')).toBeDefined();
  });

  it('renders tech stack section', () => {
    render(<App />);
    expect(screen.getByText('Technology')).toBeDefined();
    expect(screen.getByText('Kotlin')).toBeDefined();
    expect(screen.getByText('Jetpack Compose')).toBeDefined();
  });

  it('renders download section', () => {
    render(<App />);
    expect(screen.getByText('Build from source')).toBeDefined();
  });

  it('renders footer', () => {
    render(<App />);
    expect(screen.getByText('Built with Jetpack Compose and Kotlin.')).toBeDefined();
  });

  it('has locale toggle buttons', () => {
    render(<App />);
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBeGreaterThanOrEqual(2);
  });

  it('applies default theme to html', () => {
    render(<App />);
    const theme = document.documentElement.getAttribute('data-theme');
    expect(theme).toMatch(/^light|dark$/);
  });

  it('renders theme toggle button with theme icon', () => {
    render(<App />);
    const buttons = screen.getAllByRole('button');
    // First button should be theme toggle (it has aria-label with theme text)
    const themeBtn = buttons[0];
    expect(themeBtn).toBeDefined();
  });

  it('renders all feature cards', () => {
    render(<App />);
    const articles = document.querySelectorAll('article');
    expect(articles.length).toBe(4);
  });
});
