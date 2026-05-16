import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useTheme } from '../hooks/useTheme';

describe('useTheme', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('defaults to auto mode', () => {
    const { result } = renderHook(() => useTheme());
    expect(result.current.mode).toBe('auto');
  });

  it('reads saved theme from localStorage', () => {
    window.localStorage.setItem('kofeino-theme', 'dark');
    const { result } = renderHook(() => useTheme());
    expect(result.current.mode).toBe('dark');
  });

  it('setMode changes the theme mode', () => {
    const { result } = renderHook(() => useTheme());
    act(() => result.current.setMode('dark'));
    expect(result.current.mode).toBe('dark');
  });

  it('cycleMode cycles light -> dark -> auto -> light', () => {
    const { result } = renderHook(() => useTheme());

    // Start auto -> set to light
    act(() => result.current.setMode('light'));
    expect(result.current.mode).toBe('light');

    act(() => result.current.cycleMode());
    expect(result.current.mode).toBe('dark');

    act(() => result.current.cycleMode());
    expect(result.current.mode).toBe('auto');

    act(() => result.current.cycleMode());
    expect(result.current.mode).toBe('light');
  });

  it('persists theme to localStorage', () => {
    const { result } = renderHook(() => useTheme());
    act(() => result.current.setMode('dark'));
    expect(window.localStorage.getItem('kofeino-theme')).toBe('dark');
  });

  it('applies data-theme attribute to html', () => {
    const { result } = renderHook(() => useTheme());
    act(() => result.current.setMode('dark'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    act(() => result.current.setMode('light'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
