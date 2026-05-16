import type { I18nMessages } from '../types';
import styles from '../styles/hero.module.css';

interface HeroProps {
  messages: I18nMessages;
}

export default function Hero({ messages }: HeroProps) {
  return (
    <section className={styles.hero}>
      <div className={styles.inner}>
        <h1 className={styles.title}>{messages.hero.title}</h1>
        <p className={styles.subtitle}>{messages.hero.subtitle}</p>
        <a href="#download" className={styles.cta}>
          {messages.hero.cta}
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <polyline points="19 12 12 19 5 12" />
          </svg>
        </a>
      </div>
    </section>
  );
}
