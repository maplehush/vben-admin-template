import { describe, expect, it } from 'vitest';

import { canChangeUserStatus } from './status';

describe('user status switch', () => {
  const row = { id: '2', systemAccount: false };

  it('allows authorized changes to another ordinary user', () => {
    expect(canChangeUserStatus(row, '1', true, false)).toBe(true);
  });

  it('disables changes to self and changes without permission', () => {
    expect(canChangeUserStatus(row, '2', true, true)).toBe(false);
    expect(canChangeUserStatus(row, '1', false, true)).toBe(false);
  });

  it('protects a system account from delegated administrators', () => {
    const systemAccount = { id: '3', systemAccount: true };
    expect(canChangeUserStatus(systemAccount, '1', true, false)).toBe(false);
    expect(canChangeUserStatus(systemAccount, '1', true, true)).toBe(true);
  });
});
