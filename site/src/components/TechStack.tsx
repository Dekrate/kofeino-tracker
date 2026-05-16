import type { I18nMessages } from '../types';
import styles from '../styles/techstack.module.css';

interface TechStackProps {
  messages: I18nMessages;
}

interface Tech {
  name: string;
  category: string;
}

const technologies: Tech[] = [
  { name: 'Kotlin', category: 'language' },
  { name: 'Jetpack Compose', category: 'ui' },
  { name: 'Wear Material 3', category: 'ui' },
  { name: 'MVVM', category: 'architecture' },
  { name: 'Hilt DI', category: 'di' },
  { name: 'Room DB', category: 'database' },
  { name: 'Retrofit', category: 'network' },
  { name: 'Coroutines', category: 'async' },
  { name: 'StateFlow', category: 'reactive' },
  { name: 'GitHub Actions', category: 'ci' },
];

export default function TechStack({ messages }: TechStackProps) {
  return (
    <section id="tech">
      <div className="container">
        <span className="section-label">{messages.tech.heading}</span>
        <div className={styles.grid}>
          {technologies.map((tech) => (
            <span key={tech.name} className={styles.badge}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="16 18 22 12 16 6" />
                <polyline points="8 6 2 12 8 18" />
              </svg>
              {tech.name}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}
