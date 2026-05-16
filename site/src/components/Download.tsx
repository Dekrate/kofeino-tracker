import type { I18nMessages } from '../types';

interface DownloadProps {
  messages: I18nMessages;
}

export default function Download({ messages }: DownloadProps) {
  return (
    <section id="download">
      <div className="container">
        <span className="section-label">{messages.download.heading}</span>
        <p>{messages.download.description}</p>
        <div>
          {messages.download.steps.map((step, i) => (
            <p key={i} style={{ marginBottom: '0.5rem' }}>
              <strong>{i + 1}.</strong> {step}
            </p>
          ))}
        </div>
        <a
          href="https://github.com/dekrate/KofeinoTracker"
          target="_blank"
          rel="noopener noreferrer"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.75rem 1.75rem',
            background: 'var(--espresso)',
            color: 'var(--foam)',
            fontWeight: 600,
            fontSize: '0.9375rem',
            fontFamily: "'SF Pro Text', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            borderRadius: 4,
            marginTop: '1rem',
          }}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
          </svg>
          GitHub
        </a>
      </div>
    </section>
  );
}
