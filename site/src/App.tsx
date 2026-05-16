import { useLocale } from './hooks/useLocale';
import { useTheme } from './hooks/useTheme';
import Header from './components/Header';
import Hero from './components/Hero';
import Features from './components/Features';
import TechStack from './components/TechStack';
import Download from './components/Download';
import Footer from './components/Footer';
import styles from './styles/app.module.css';

export default function App() {
  const { messages, toggleLocale } = useLocale();
  const { mode: themeMode, cycleMode: onCycleTheme } = useTheme();

  return (
    <div className={styles.page}>
      <Header
        messages={messages}
        onToggleLocale={toggleLocale}
        themeMode={themeMode}
        onCycleTheme={onCycleTheme}
      />
      <main>
        <Hero messages={messages} />
        <Features messages={messages} />
        <TechStack messages={messages} />
        <Download messages={messages} />
      </main>
      <Footer messages={messages} />
    </div>
  );
}
