export type Locale = 'pl' | 'en';

export type ThemeMode = 'light' | 'dark' | 'auto';

export interface I18nMessages {
  nav: {
    features: string;
    tech: string;
    download: string;
  };
  hero: {
    title: string;
    subtitle: string;
    cta: string;
  };
  features: {
    heading: string;
    items: Array<{
      title: string;
      description: string;
    }>;
  };
  tech: {
    heading: string;
  };
  screenshots: {
    heading: string;
  };
  download: {
    heading: string;
    description: string;
    steps: string[];
  };
  footer: {
    built: string;
    license: string;
  };
  locale: {
    toggle: string;
  };
  theme: {
    light: string;
    dark: string;
    auto: string;
  };
}
