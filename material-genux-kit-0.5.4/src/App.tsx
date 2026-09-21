import { useState, useEffect } from 'react';
import { Button } from '@gbreeze/components/button/button';
import { IconButton } from '@gbreeze/components/iconbutton/icon-button';
import { Snackbar } from '@gbreeze/components/snackbar/snackbar';

export default function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() =>
    window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light',
  );
  const [isSnackbarOpen, setIsSnackbarOpen] = useState(false);

  useEffect(() => {
    document.documentElement.style.colorScheme = theme;
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  return (
    <div className="bg-surface text-on-surface min-h-screen flex flex-col gap-4 items-center justify-center relative">
      <div className="absolute top-4 right-4">
        <IconButton
          icon={theme === 'light' ? 'dark_mode' : 'light_mode'}
          onClick={toggleTheme}
          color="standard"
          aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
        />
      </div>
      <h1 className="typescale-display-md my-4 text-primary">Hello Material</h1>
      <p className="typescale-body-lg text-on-surface-variant max-w-2xl text-center">
        Enter a prompt to generate a Material app.
      </p>

      <div className="flex flex-col gap-4 max-w-2xl text-center p-6 bg-surface-container text-on-surface rounded-2xl">
        <h2 className="typescale-headline-md text-primary">Getting Started</h2>
        <p className="typescale-body-md text-on-surface-variant">
          To get started, ensure you have installed{' '}
          <a
            href="https://nodejs.org/en/download/"
            className="text-primary hover:underline"
            target="_blank"
            rel="noopener noreferrer"
          >
            Node.js
          </a>
          .
        </p>
        <details className="text-center w-full max-w-lg mx-auto">
          <summary className="typescale-body-md text-on-surface-variant cursor-pointer hover:text-primary">
            Ensure browser tools are enabled in Jetski.
          </summary>
          <ul className="list-disc list-inside ml-4 mt-2 typescale-body-sm text-on-surface-variant text-left">
            <li>
              Press "⌘ + ," (or click "Jetski &gt; Settings &gt; Jetski
              Settings" in the menu bar)
            </li>
            <li>Click Browser in the sidebar</li>
            <li>Enable "Browser Tools"</li>
            <li>
              <a
                href="https://screencast.googleplex.com/cast/NjY4NzU2MDM4OTYyMzgwOHw4MTdhYzcxNi0zMA"
                className="text-primary hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                Video Example
              </a>
            </li>
          </ul>
        </details>
        <p className="typescale-body-md text-on-surface-variant">
          Then, tell the agent what you want to build.
        </p>
        <p className="typescale-body-md text-on-surface-variant">
          Jetski will initialize the project, open Chrome, and start building!
        </p>
      </div>

      <div className="flex flex-col gap-4 max-w-2xl">
        <h2 className="typescale-headline-md text-center text-primary">
          Example Prompts
        </h2>
        <ul className="flex flex-col gap-4">
          {[
            'Show me a dashboard of AI usage for my team.',
            'Make a simple calculator app.',
            'Build the Youtube frontpage, but make it use Material Design.',
          ].map((prompt, index) => (
            <li
              key={index}
              className="flex items-center gap-4 bg-surface-container p-4 rounded-xl"
            >
              <span className="flex-1 typescale-body-md text-on-surface-variant">
                {prompt}
              </span>
              <Button
                label="Copy"
                icon="file_copy"
                size="sm"
                color="tonal"
                onClick={() => {
                  navigator.clipboard.writeText(prompt);
                  setIsSnackbarOpen(true);
                }}
              />
            </li>
          ))}
        </ul>
      </div>

      <Snackbar
        message="Copied to the clipboard!"
        isOpen={isSnackbarOpen}
        onDismiss={() => setIsSnackbarOpen(false)}
        duration={2000}
      />

      <div className="flex gap-6">
        <Button label="Prompt" color="outlined" />
        <Button label="Review" color="tonal" />
        <Button label="Prototype" color="filled" />
      </div>
    </div>
  );
}
