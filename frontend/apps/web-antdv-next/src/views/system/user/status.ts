import type { UserSummary } from '#/api/system';

export function canChangeUserStatus(
  row: Pick<UserSummary, 'id' | 'systemAccount'>,
  actorId: string | undefined,
  hasPermission: boolean,
  isSuperAdmin: boolean,
): boolean {
  return hasPermission && row.id !== actorId && (!row.systemAccount || isSuperAdmin);
}
