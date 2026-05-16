import { useState, useCallback, useEffect } from 'react';
import type { Locale } from '../types';
import { getMessages, detectBrowserLocale } from '../i18n';
import type { I18nMessages } from '../types';

const STORAGE_KEY = 'kofeino-locale';

function loadSavedLocale(): Locale | null {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'pl' || saved === 'en') return saved;
  } catch {
    // localStorage may be blocked
  }
  return null;
}

function saveLocale(locale: Locale): void {
  try {
    localStorage.setItem(STORAGE_KEY, locale);
  } catch {
    // silently fail
  }
}

export function useLocale() {
  const [locale, setLocaleState] = useState<Locale>(() => {
    return loadSavedLocale() ?? detectBrowserLocale();
  });

  const [messages, setMessages] = useState<I18nMessages>(() => getMessages(locale));

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    setMessages(getMessages(next));
    saveLocale(next);
  }, []);

  const toggleLocale = useCallback(() => {
    setLocale(locale === 'pl' ? 'en' : 'pl');
  }, [locale, setLocale]);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  return { locale, messages, setLocale, toggleLocale };
}
