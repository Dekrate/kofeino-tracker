import { describe, it, expect } from 'vitest';
import { getMessages, detectBrowserLocale } from '../i18n';

describe('getMessages', () => {
  it('returns English messages for "en"', () => {
    const msgs = getMessages('en');
    expect(msgs.hero.title).toBe('KofeinoTracker');
    expect(msgs.nav.features).toBe('Features');
  });

  it('returns Polish messages for "pl"', () => {
    const msgs = getMessages('pl');
    expect(msgs.hero.title).toBe('KofeinoTracker');
    expect(msgs.nav.features).toBe('Funkcje');
  });

  it('falls back to English for unknown locale', () => {
    const msgs = getMessages('de' as never);
    expect(msgs.hero.title).toBe('KofeinoTracker');
    expect(msgs.nav.features).toBe('Features');
  });

  it('contains all required sections', () => {
    const msgs = getMessages('en');
    expect(msgs.nav).toBeDefined();
    expect(msgs.hero).toBeDefined();
    expect(msgs.features).toBeDefined();
    expect(msgs.tech).toBeDefined();
    expect(msgs.screenshots).toBeDefined();
    expect(msgs.download).toBeDefined();
    expect(msgs.footer).toBeDefined();
    expect(msgs.locale).toBeDefined();
  });

  it('has matching structure in both locales', () => {
    const en = getMessages('en');
    const pl = getMessages('pl');
    expect(Object.keys(en)).toEqual(Object.keys(pl));
    expect(en.features.items).toHaveLength(pl.features.items.length);
    expect(en.download.steps).toHaveLength(pl.download.steps.length);
  });
});

describe('detectBrowserLocale', () => {
  it('returns "en" when navigator.language starts with "en"', () => {
    Object.defineProperty(navigator, 'language', {
      value: 'en-US',
      configurable: true,
    });
    expect(detectBrowserLocale()).toBe('en');
  });

  it('returns "pl" when navigator.language starts with "pl"', () => {
    Object.defineProperty(navigator, 'language', {
      value: 'pl-PL',
      configurable: true,
    });
    expect(detectBrowserLocale()).toBe('pl');
  });

  it('returns "en" for unsupported languages', () => {
    Object.defineProperty(navigator, 'language', {
      value: 'de-DE',
      configurable: true,
    });
    expect(detectBrowserLocale()).toBe('en');
  });

  it('handles missing navigator.language gracefully', () => {
    Object.defineProperty(navigator, 'language', {
      get: () => { throw new Error('unavailable'); },
      configurable: true,
    });
    expect(detectBrowserLocale()).toBe('en');
  });
});
