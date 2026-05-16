import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useLocale } from '../hooks/useLocale';

describe('useLocale', () => {
  beforeEach(() => {
    Object.defineProperty(navigator, 'language', {
      value: 'en-US',
      configurable: true,
    });
    window.localStorage.clear();
  });

  it('detects browser locale on init', () => {
    const { result } = renderHook(() => useLocale());
    expect(result.current.locale).toBe('en');
  });

  it('reads saved locale from localStorage', () => {
    window.localStorage.setItem('kofeino-locale', 'pl');
    const { result } = renderHook(() => useLocale());
    expect(result.current.locale).toBe('pl');
  });

  it('returns English messages for en locale', () => {
    const { result } = renderHook(() => useLocale());
    expect(result.current.messages.nav.features).toBe('Features');
  });

  it('returns Polish messages for pl locale', () => {
    window.localStorage.setItem('kofeino-locale', 'pl');
    const { result } = renderHook(() => useLocale());
    expect(result.current.messages.nav.features).toBe('Funkcje');
  });

  it('toggleLocale switches between pl and en', () => {
    const { result } = renderHook(() => useLocale());
    expect(result.current.locale).toBe('en');

    act(() => result.current.toggleLocale());
    expect(result.current.locale).toBe('pl');
    expect(result.current.messages.nav.features).toBe('Funkcje');

    act(() => result.current.toggleLocale());
    expect(result.current.locale).toBe('en');
    expect(result.current.messages.nav.features).toBe('Features');
  });

  it('setLocale changes locale and messages', () => {
    const { result } = renderHook(() => useLocale());
    act(() => result.current.setLocale('pl'));
    expect(result.current.locale).toBe('pl');
    expect(result.current.messages.hero.subtitle).toBe('Śledź kofeinę, kontroluj dzień.');
  });

  it('persists locale to localStorage', () => {
    const { result } = renderHook(() => useLocale());
    act(() => result.current.setLocale('pl'));
    expect(window.localStorage.getItem('kofeino-locale')).toBe('pl');
  });

  it('sets html lang attribute', () => {
    const { result } = renderHook(() => useLocale());
    expect(document.documentElement.lang).toBe('en');
    act(() => result.current.setLocale('pl'));
    expect(document.documentElement.lang).toBe('pl');
  });
});
