import { useEffect, useState } from 'react';
import { cn } from '@konfigyr/components/utils';

export function ProgressLoader({ className }: { className?: string }) {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setWidth((prev) => {
        // Stop at 95% until completion
        if (prev >= 95) return prev;

        // Slower progress as we get higher (realistic loading)
        const increment = Math.random() * (100 - prev) * 0.1;
        const slowdown = prev > 70 ? 0.3 : prev > 50 ? 0.6 : 1;

        return Math.min(prev + increment * slowdown, 95);
      });
    }, 600);

    return () => clearInterval(interval);
  }, []);

  return (
    <div className={cn('relative bg-gray-200 h-0.5 w-[18rem] z-50 pointer-events-none rounded-full', className)}>
      <div
        className="h-full bg-secondary transition-all duration-300 ease-out shadow-sm"
        style={{ width: `${width}%` }}
      />
    </div>
  );
}
