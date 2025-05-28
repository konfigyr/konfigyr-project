'use client';

import { Avatar, AvatarImage, AvatarFallback } from 'konfigyr/components/ui';

export function AccountAvatar({ name, picture }: { name?: string, picture?: string }) {
  return (
    <Avatar>
      <AvatarImage src={picture} />
      <AvatarFallback>{name}</AvatarFallback>
    </Avatar>
  );
}
