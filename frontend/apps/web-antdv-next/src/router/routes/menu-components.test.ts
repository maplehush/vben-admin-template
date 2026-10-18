import { describe, expect, it } from 'vitest';

import { menuComponentOptions, toMenuComponentOptions } from './menu-components';

describe('menu component options', () => {
  it('bundles the dictionary management page for backend menus', () => {
    expect(menuComponentOptions).toContainEqual({
      label: '/system/dict/list',
      value: '/system/dict/list',
    });
  });

  it('lists actual page components and excludes internal and form components', () => {
    expect(
      toMenuComponentOptions([
        '../../views/system/user/list.vue',
        '../../views/dashboard/workspace/index.vue',
        '../../views/_core/authentication/login.vue',
        '../../views/system/user/modules/form.vue',
        '../../views/business/order/components/filter.vue',
      ]),
    ).toEqual([
      {
        label: '/dashboard/workspace/index',
        value: '/dashboard/workspace/index',
      },
      { label: '/system/user/list', value: '/system/user/list' },
    ]);
  });
});
