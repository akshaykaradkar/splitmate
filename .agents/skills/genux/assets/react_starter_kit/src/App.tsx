import {AppBar} from '@/gbreeze/components/appbar/app-bar';
import {Button} from '@/gbreeze/components/button/button';
import {Card} from '@/gbreeze/components/card/card';
import {IconButton} from '@/gbreeze/components/iconbutton/icon-button';
import {List, ListItem} from '@/gbreeze/components/list/list';
import {useEffect, useState} from 'react';

/**
 * The main application.
 */
export function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() =>
    window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light',
  );

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
    <div className="bg-surface text-on-surface min-h-screen flex flex-col">
      <header role="banner">
        <AppBar
          title="Hello Material"
          variant="standard"
          trailing={
            <div className="flex items-center gap-2">
              <Button
                label="Component Catalog"
                icon="menu_book"
                color="tonal"
                href="./storybook/"
                target="_blank"
                rel="noopener noreferrer"
              />
              <IconButton
                icon={theme === 'light' ? 'dark_mode' : 'light_mode'}
                onClick={toggleTheme}
                color="standard"
                aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
              />
            </div>
          }
        />
      </header>

      <main
        role="main"
        className="flex-1 w-full max-w-2xl mx-auto p-6 flex flex-col gap-6">
        <p className="typescale-body-lg text-on-surface-variant">
          Enter a prompt to generate a Material app using pre-built gBreeze
          components.
        </p>

        <Card color="filled">
          <div className="p-4">
            <List>
              {[
                'Show me a dashboard of AI usage for my team.',
                'Make a simple calculator app.',
                'Build the Youtube frontpage, but make it use Material Design.',
              ].map((prompt, index) => (
                <ListItem
                  key={index}
                  label={prompt}
                  trailingContent={
                    <Button
                      label="Copy"
                      icon="file_copy"
                      color="tonal"
                      onClick={() => {
                        navigator.clipboard.writeText(prompt);
                      }}
                    />
                  }
                />
              ))}
            </List>
          </div>
        </Card>
      </main>
    </div>
  );
}
