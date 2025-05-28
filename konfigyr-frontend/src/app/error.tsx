'use client';

import ErrorComponent from 'konfigyr/components/error';

export default function ErrorPage({ error }: { error: Error & { digest?: string } }) {
  return (
    <div className="container px-4">
      <div className="w-full md:w-2/3 lg:w-1/2 mx-auto py-4">
        <ErrorComponent error={error}/>
      </div>
    </div>
  );
}
