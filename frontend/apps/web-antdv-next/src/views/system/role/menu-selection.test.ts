import { describe, expect, it } from 'vitest';

import { enabledMenuIds, sameMenuIds } from './menu-selection';

describe('role menu selection', () => {
  it('selects every enabled menu and button at every level', () => {
    const node = (id: string, children: any[] = [], status = 'ENABLED') =>
      ({ id, children, status }) as any;
    expect(enabledMenuIds([
      node('root', [node('page', [node('button')]), node('disabled', [node('child')], 'DISABLED')]),
    ])).toEqual(['root', 'page', 'button']);
  });

  it('detects an incomplete server readback regardless of order', () => {
    expect(sameMenuIds(['1', '2'], ['2', '1'])).toBe(true);
    expect(sameMenuIds(['1', '2'], ['1'])).toBe(false);
  });
});
