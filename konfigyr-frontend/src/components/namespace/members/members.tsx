import { useCallback, useState } from 'react';
import { EllipsisVerticalIcon, UserXIcon, UsersIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useGetNamespaceMembers } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Avatar, AvatarFallback, AvatarImage } from '@konfigyr/components/ui/avatar';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { NamespaceRoleBadge } from '../role';
import { RemoveMemberForm } from './remove-member';
import { UpdateMemberForm } from './update-member';

import type { Member, Namespace } from '@konfigyr/hooks/types';

export interface MemberArticleProps {
  member: Member;
  onEdit: (member: Member) => void;
  onRemove: (member: Member) => void;
}

export function SkeletonArticle() {
  return (
    <article data-slot="member-skeleton" className="flex justify-between items-center gap-4">
      <Skeleton className="size-12 rounded-full" />
      <div className="grow space-y-2">
        <Skeleton className="w-48 h-4" />
        <Skeleton className="w-64 h-3" />
      </div>
      <Skeleton className="w-18 h-4 mr-16" />
    </article>
  );
}

export function MemberArticle({ member, onEdit, onRemove }: MemberArticleProps) {
  return (
    <article data-slot="member-article" className="flex justify-between items-center gap-4">
      <Avatar size="lg">
        <AvatarImage src={member.avatar} />
        <AvatarFallback title={member.fullName} />
      </Avatar>

      <div className="grow">
        <p className="font-medium">{member.fullName}</p>
        <p className="text-sm text-muted-foreground font-mono">{member.email}</p>
      </div>

      <div>
        <NamespaceRoleBadge role={member.role} variant="outline" />
      </div>

      <div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" aria-label="More options">
              <EllipsisVerticalIcon />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem onClick={() => onEdit(member)}>
              <FormattedMessage
                defaultMessage="Update role"
                description="Label for the update role button in the member article"
              />
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onRemove(member)}>
              <FormattedMessage
                defaultMessage="Remove member"
                description="Label for the remove member button in the member article"
              />
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </article>
  );
}

export function Members({ namespace }: { namespace: Namespace }) {
  const [updating, setUpdating] = useState<Member | undefined>();
  const [removing, setRemoving] = useState<Member | undefined>();
  const { data: members, error, isPending, isError } = useGetNamespaceMembers(namespace.slug);

  const onCloseUpdateMemberForm = useCallback(() => setUpdating(undefined), []);
  const onCloseRemoveMemberForm = useCallback(() => setRemoving(undefined), []);

  return (
    <>
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CardIcon>
              <UsersIcon size="1.25rem"/>
            </CardIcon>
            <FormattedMessage
              defaultMessage="Namespace members"
              description="Title used in the card that lists all namespace members."
            />
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6">
            {isPending && (
              <SkeletonArticle />
            )}

            {isError && (
              <ErrorState error={error} className="border-none" />
            )}

            {members?.length === 0 && (
              <EmptyState
                title="Your namespace has no members yet."
                description="Invite your team members to join your namespace and start your collaboration right away."
                icon={<UserXIcon />}
              />
            )}

            {members?.map(member => (
              <MemberArticle
                key={member.id}
                member={member}
                onEdit={setUpdating}
                onRemove={setRemoving}
              />
            ))}
          </div>
        </CardContent>
      </Card>

      <UpdateMemberForm
        member={updating}
        namespace={namespace}
        onClose={onCloseUpdateMemberForm}
      />

      <RemoveMemberForm
        member={removing}
        namespace={namespace}
        onClose={onCloseRemoveMemberForm}
      />
    </>
  );
}
