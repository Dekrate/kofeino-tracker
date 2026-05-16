import type { I18nMessages } from '../types';

const themeStrings = {
  light: 'Light',
  dark: 'Dark',
  auto: 'Auto',
};

const themeStringsPl = {
  light: 'Jasny',
  dark: 'Ciemny',
  auto: 'Auto',
};

const en: I18nMessages = {
  nav: {
    features: 'Features',
    tech: 'Tech Stack',
    download: 'Get Started',
  },
  hero: {
    title: 'KofeinoTracker',
    subtitle: 'Track your caffeine, own your day.',
    cta: 'Get Started',
  },
  features: {
    heading: 'Built for your wrist',
    items: [
      {
        title: 'Wear OS Native',
        description:
          'Purpose-built for Wear OS with swipe gestures, circular layouts, and a native watch face experience.',
      },
      {
        title: 'Real Food Data',
        description:
          'Caffeine values sourced from Open Food Facts — a crowd-sourced database of real products. Works offline with local cache.',
      },
      {
        title: 'Smart Dashboard',
        description:
          'Circular progress shows your daily total at a glance. Quick-add drinks, track history, and get warned when you exceed 400 mg.',
      },
      {
        title: 'Fully Offline',
        description:
          'All data stored locally on your watch. Room database with coroutines for thread-safe, instant access.',
      },
    ],
  },
  tech: {
    heading: 'Technology',
  },
  screenshots: {
    heading: 'On your wrist',
  },
  download: {
    heading: 'Build from source',
    description:
      'KofeinoTracker is open-source. Clone the repo, open in Android Studio, and deploy to your Wear OS device.',
    steps: [
      'Clone the repository from GitHub',
      'Open the project in Android Studio',
      'Select the "wear" run configuration',
      'Build and deploy to your Wear OS emulator or device',
    ],
  },
  footer: {
    built: 'Built with Jetpack Compose and Kotlin.',
    license: 'MIT License',
  },
  locale: {
    toggle: 'Przełącz na polski',
  },
  theme: themeStrings,
};

const pl: I18nMessages = {
  nav: {
    features: 'Funkcje',
    tech: 'Technologie',
    download: 'Start',
  },
  hero: {
    title: 'KofeinoTracker',
    subtitle: 'Śledź kofeinę, kontroluj dzień.',
    cta: 'Rozpocznij',
  },
  features: {
    heading: 'Zaprojektowane na nadgarstek',
    items: [
      {
        title: 'Natywnie na Wear OS',
        description:
          'Stworzone specjalnie dla Wear OS z gestami przesunięcia, okrągłymi układami i natywnym interfejsem zegarka.',
      },
      {
        title: 'Rzeczywiste dane o kofeinie',
        description:
          'Wartości kofeiny pochodzą z Open Food Facts — otwartej bazy prawdziwych produktów. Działa offline z lokalnym cache.',
      },
      {
        title: 'Inteligentny pulpit',
        description:
          'Okrągły wskaźnik postępu pokazuje dzienne spożycie na pierwszy rzut oka. Szybkie dodawanie, historia i ostrzeżenie po przekroczeniu 400 mg.',
      },
      {
        title: 'W pełni offline',
        description:
          'Wszystkie dane przechowywane lokalnie na zegarku. Baza Room z coroutines dla bezpiecznego i błyskawicznego dostępu.',
      },
    ],
  },
  tech: {
    heading: 'Technologie',
  },
  screenshots: {
    heading: 'Na twoim nadgarstku',
  },
  download: {
    heading: 'Zbuduj ze źródła',
    description:
      'KofeinoTracker ma otwarte źródło. Sklonuj repozytorium, otwórz w Android Studio i wdróż na swoim Wear OS.',
    steps: [
      'Sklonuj repozytorium z GitHub',
      'Otwórz projekt w Android Studio',
      'Wybierz konfigurację "wear"',
      'Zbuduj i wdróż na emulatorze lub zegarku Wear OS',
    ],
  },
  footer: {
    built: 'Zbudowane z Jetpack Compose i Kotlin.',
    license: 'Licencja MIT',
  },
  locale: {
    toggle: 'Switch to English',
  },
  theme: themeStringsPl,
};

const messages: Record<string, I18nMessages> = { en, pl };

export function getMessages(locale: string): I18nMessages {
  const msgs = messages[locale];
  if (!msgs) {
    console.warn(`Unsupported locale "${locale}", falling back to "en"`);
    return messages.en as I18nMessages;
  }
  return msgs;
}

export function detectBrowserLocale(): 'pl' | 'en' {
  try {
    const raw = navigator.language ?? navigator.languages?.[0] ?? 'en';
    const lang = raw.slice(0, 2).toLowerCase();
    if (lang === 'pl') return 'pl';
    return 'en';
  } catch {
    return 'en';
  }
}
