import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createRouterGuard } from './guard';

const mocks = vi.hoisted(() => {
  const accessStore = {
    accessCodes: ['system:resource:create'],
    accessRoutes: [] as Array<{
      children?: Array<{ name: string; path: string }>;
      name: string;
      path: string;
    }>,
    accessToken: 'token',
    isAccessChecked: false,
    setAccessCodes: vi.fn(),
    setAccessMenus: vi.fn(),
    setAccessRoutes: vi.fn(),
    setIsAccessChecked: vi.fn(),
  };
  const userInfo = {
    homePath: '/system/menu',
    mustChangePassword: false,
    roles: ['super_admin'],
  };
  const tabbarStore = {
    tabs: [] as Array<{ key?: string; name: string; path: string }>,
    _bulkCloseByKeys: vi.fn(async (_keys: string[]) => undefined),
  };
  return {
    accessStore,
    tabbarStore,
    getAccessCodesApi: vi.fn(async () => ['system:menu:create']),
    generateAccess: vi.fn(async () => ({
      accessibleMenus: [],
      accessibleRoutes: [] as Array<{
        children?: Array<{ name: string; path: string }>;
        name: string;
        path: string;
      }>,
    })),
    userInfo,
    userStore: { userInfo },
  };
});

vi.mock('@vben/constants', () => ({ LOGIN_PATH: '/auth/login' }));
vi.mock('@vben/preferences', () => ({
  preferences: {
    app: { defaultHomePath: '/dashboard/workspace' },
    transition: { progress: false },
  },
}));
vi.mock('@vben/stores', () => ({
  getTabKey: (tab: { path: string }) => tab.path,
  useAccessStore: () => mocks.accessStore,
  useTabbarStore: () => mocks.tabbarStore,
  useUserStore: () => mocks.userStore,
}));
vi.mock('@vben/utils', () => ({ startProgress: vi.fn(), stopProgress: vi.fn() }));
vi.mock('#/api', () => ({ getAccessCodesApi: mocks.getAccessCodesApi }));
vi.mock('#/router/routes', () => ({ accessRoutes: [], coreRouteNames: [] }));
vi.mock('#/store', () => ({
  useAuthStore: () => ({ fetchUserInfo: vi.fn(async () => mocks.userInfo) }),
}));
vi.mock('./access', () => ({ generateAccess: mocks.generateAccess }));

describe('access guard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.accessStore.isAccessChecked = false;
    mocks.accessStore.accessCodes = ['system:resource:create'];
    mocks.accessStore.accessRoutes = [];
    mocks.tabbarStore.tabs = [];
  });

  it('replaces persisted permission codes when restoring a session', async () => {
    const beforeEachCallbacks: Array<(to: any, from: any) => Promise<any>> = [];
    const router = {
      afterEach: vi.fn(),
      beforeEach: (callback: (to: any, from: any) => Promise<any>) => {
        beforeEachCallbacks.push(callback);
      },
      resolve: (path: string) => ({ fullPath: path }),
    };
    createRouterGuard(router as never);

    await beforeEachCallbacks.at(-1)?.(
      {
        fullPath: '/system/menu',
        meta: {},
        name: 'SystemMenu',
        path: '/system/menu',
      },
      { query: {} },
    );

    expect(mocks.getAccessCodesApi).toHaveBeenCalledOnce();
    expect(mocks.accessStore.setAccessCodes).toHaveBeenCalledWith([
      'system:menu:create',
    ]);
  });

  it('closes persisted tabs that the current account cannot access', async () => {
    mocks.tabbarStore.tabs = [
      { key: 'workspace', name: 'Workspace', path: '/dashboard/workspace' },
      { key: 'old-user', name: 'SystemUser', path: '/system/user' },
      { key: 'profile', name: 'Profile', path: '/profile' },
    ];
    mocks.generateAccess.mockResolvedValueOnce({
      accessibleMenus: [],
      accessibleRoutes: [
        {
          name: 'Dashboard',
          path: '/dashboard',
          children: [{ name: 'Workspace', path: '/dashboard/workspace' }],
        },
      ],
    });
    const callbacks: Array<(to: any, from: any) => Promise<any>> = [];
    const router = {
      afterEach: vi.fn(),
      beforeEach: (callback: (to: any, from: any) => Promise<any>) => {
        callbacks.push(callback);
      },
      resolve: (path: string) => ({ fullPath: path }),
    };
    createRouterGuard(router as never);

    await callbacks.at(-1)?.(
      { fullPath: '/dashboard/workspace', meta: {}, name: 'Workspace', path: '/dashboard/workspace' },
      { query: {} },
    );

    expect(mocks.tabbarStore._bulkCloseByKeys).toHaveBeenCalledWith(['old-user']);
  });

  it('rejects a retained route without a menu grant even when API codes exist', async () => {
    mocks.accessStore.isAccessChecked = true;
    mocks.accessStore.accessRoutes = [
      {
        name: 'Dashboard',
        path: '/dashboard',
        children: [{ name: 'Workspace', path: '/dashboard/workspace' }],
      },
    ];
    mocks.userInfo.homePath = '/dashboard/workspace';
    const beforeEachCallbacks: Array<(to: any, from: any) => Promise<any>> = [];
    createRouterGuard({
      afterEach: vi.fn(),
      beforeEach: (callback: (to: any, from: any) => Promise<any>) => {
        beforeEachCallbacks.push(callback);
      },
    } as never);

    const result = await beforeEachCallbacks.at(-1)?.(
      {
        fullPath: '/system/user',
        meta: {},
        name: 'SystemUser',
        path: '/system/user',
      },
      { query: {} },
    );

    expect(result).toBe('/dashboard/workspace');
  });
});
