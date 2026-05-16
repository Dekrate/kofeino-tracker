import type { I18nMessages } from '../types';
import styles from '../styles/features.module.css';

interface FeaturesProps {
  messages: I18nMessages;
}

function WatchIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="5" y="2" width="14" height="20" rx="3" />
      <line x1="9" y1="6" x2="15" y2="6" />
      <line x1="9" y1="18" x2="15" y2="18" />
    </svg>
  );
}

function DatabaseIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <ellipse cx="12" cy="5" rx="9" ry="3" />
      <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
      <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
    </svg>
  );
}

function GaugeIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <path d="M12 8v4l3 3" />
      <circle cx="12" cy="12" r="1" fill="currentColor" />
    </svg>
  );
}

function SignalIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
    </svg>
  );
}

const featureIcons = [WatchIcon, DatabaseIcon, GaugeIcon, SignalIcon];

export default function Features({ messages }: FeaturesProps) {
  return (
    <section id="features" className={styles.section}>
      <div className="container">
        <span className="section-label">{messages.features.heading}</span>
        <div className={styles.grid}>
          {messages.features.items.map((item, i) => {
            const Icon = featureIcons[i];
            return (
              <article key={item.title} className={styles.card}>
                <div className={styles.icon}>
                  {Icon ? <Icon /> : null}
                </div>
                <h3 className={styles.cardTitle}>{item.title}</h3>
                <p className={styles.cardDesc}>{item.description}</p>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
