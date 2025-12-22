'use client';

import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useTheme } from 'next-themes';
import { MoonIcon, SunIcon } from 'lucide-react';

import { Button } from '@konfigyr/components/ui/button';

export function ThemeSwitcher() {
  const { setTheme, resolvedTheme } = useTheme();

  const toggleTheme = useCallback(() => {
    setTheme(resolvedTheme === 'dark' ? 'light' : 'dark');
  }, [resolvedTheme, setTheme]);

  const Icon = resolvedTheme  ? SunIcon : MoonIcon;

  return (
    <Button
      variant="ghost"
      size="icon"
      className="group/toggle extend-touch-target size-8"
      onClick={toggleTheme}
      title="Toggle theme"
    >
      <Icon size="1rem" />
      <span className="sr-only">
        <FormattedMessage
          defaultMessage="Toggle theme"
          description="Label for the theme switcher button"
        />
      </span>
    </Button>
  );
}
