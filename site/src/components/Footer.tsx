import type { I18nMessages } from '../types';
import styles from '../styles/footer.module.css';

interface FooterProps {
  messages: I18nMessages;
}

export default function Footer({ messages }: FooterProps) {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <p className={styles.text}>{messages.footer.built}</p>
        <a
          href="https://github.com/dekrate/KofeinoTracker"
          target="_blank"
          rel="noopener noreferrer"
          className={styles.link}
        >
          GitHub
        </a>
        <span className={styles.text}>{messages.footer.license}</span>
      </div>
    </footer>
  );
}
