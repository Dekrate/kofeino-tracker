import { useState, useCallback, useEffect } from 'react';
import type { ThemeMode } from '../types';

const STORAGE_KEY = 'kofeino-theme';

function getSystemTheme(): 'light' | 'dark' {
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  } catch {
    return 'light';
  }
}

function loadSavedTheme(): ThemeMode | null {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'light' || saved === 'dark' || saved === 'auto') {
      return saved;
    }
  } catch {
    // localStorage blocked
  }
  return null;
}

function saveTheme(mode: ThemeMode): void {
  try {
    localStorage.setItem(STORAGE_KEY, mode);
  } catch {
    // silently fail
  }
}

function resolveTheme(mode: ThemeMode): 'light' | 'dark' {
  if (mode === 'auto') return getSystemTheme();
  return mode;
}

export function useTheme() {
  const [mode, setModeState] = useState<ThemeMode>(() => loadSavedTheme() ?? 'auto');

  const setMode = useCallback((next: ThemeMode) => {
    setModeState(next);
    saveTheme(next);
  }, []);

  const cycleMode = useCallback(() => {
    setModeState((prev) => {
      const next = prev === 'light' ? 'dark' : prev === 'dark' ? 'auto' : 'light';
      saveTheme(next);
      return next;
    });
  }, []);

  // Apply resolved theme to <html>
  useEffect(() => {
    const resolved = resolveTheme(mode);
    document.documentElement.setAttribute('data-theme', resolved);
  }, [mode]);

  // Listen for system theme changes in auto mode
  useEffect(() => {
    if (mode !== 'auto') return;

    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => {
      document.documentElement.setAttribute(
        'data-theme',
        mq.matches ? 'dark' : 'light',
      );
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [mode]);

  return { mode, setMode, cycleMode };
}
