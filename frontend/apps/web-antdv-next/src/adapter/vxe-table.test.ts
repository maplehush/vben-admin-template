import { createApp } from 'vue';

import { useVbenVxeGrid as useBaseGrid } from '@vben/plugins/vxe-table';

import { describe, expect, it, vi } from 'vitest';

import { useVbenVxeGrid, VbenTableAction } from './vxe-table';

vi.mock('@vben/access', () => ({
  useAccess: () => ({
    hasAccessByCodes: (codes: string[]) => codes.includes('allowed'),
  }),
}));

vi.mock('@vben/common-ui', async () => {
  const { defineComponent, h } = await import('vue');
  return {
    VbenTableAction: defineComponent({
      props: { hasPermission: { default: undefined, type: Function } },
      setup(props) {
        return () =>
          h('span', {
            'data-allowed': String(props.hasPermission?.('allowed')),
            'data-denied': String(props.hasPermission?.('denied')),
          });
      },
    }),
  };
});

vi.mock('@vben/plugins/vxe-table', () => ({
  setupVbenVxeTable: vi.fn(),
  useVbenVxeGrid: vi.fn(),
}));

vi.mock('antdv-next', () => ({ Button: {}, Image: {} }));
vi.mock('./form', () => ({ useVbenForm: vi.fn() }));

describe('system table action', () => {
  it('injects the current user permission check', () => {
    const container = document.createElement('div');
    const app = createApp(VbenTableAction);
    app.mount(container);
    expect(container.querySelector('span')?.dataset.allowed).toBe('true');
    expect(container.querySelector('span')?.dataset.denied).toBe('false');
    app.unmount();
  });
});

describe('system table date display', () => {
  it('passes timestamp columns through without adding a formatter', () => {
    const columns = [
      { field: 'createTime', title: '创建时间' },
      { field: 'expireTime', title: '过期时间' },
      { field: 'occurTime', title: '发生时间' },
      { field: 'updateTime', formatter: 'customFormat' },
      { field: 'username', title: '用户名' },
    ];
    useVbenVxeGrid({ gridOptions: { columns } });
    const passed = vi.mocked(useBaseGrid).mock.lastCall?.[0];
    expect(passed?.gridOptions?.columns).toBe(columns);
    expect(columns[0]).not.toHaveProperty('formatter');
  });
});
