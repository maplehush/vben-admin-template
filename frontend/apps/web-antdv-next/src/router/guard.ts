import type { Router } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { getTabKey, useAccessStore, useTabbarStore, useUserStore } from '@vben/stores';
import { startProgress, stopProgress } from '@vben/utils';

import { getAccessCodesApi } from '#/api';
import { accessRoutes, coreRouteNames } from '#/router/routes';
import { useAuthStore } from '#/store';

import { generateAccess } from './access';

const CHANGE_PASSWORD_PATH = '/auth/change-password';
const PROFILE_PATH = '/profile';

function hasAccessibleRoute(routes: RouteRecordRaw[], name: unknown): boolean {
  return routes.some(
    (route) =>
      route.name === name ||
      (route.children && hasAccessibleRoute(route.children, name)),
  );
}

/**
 * 通用守卫配置
 * @param router
 */
function setupCommonGuard(router: Router) {
  // 记录已经加载的页面
  const loadedPaths = new Set<string>();

  router.beforeEach((to) => {
    to.meta.loaded = loadedPaths.has(to.path);

    // 页面加载进度条
    if (!to.meta.loaded && preferences.transition.progress) {
      startProgress();
    }
    return true;
  });

  router.afterEach((to) => {
    // 记录页面是否加载,如果已经加载，后续的页面切换动画等效果不在重复执行

    loadedPaths.add(to.path);

    // 关闭页面加载进度条
    if (preferences.transition.progress) {
      stopProgress();
    }
  });
}

/**
 * 权限访问守卫配置
 * @param router
 */
function setupAccessGuard(router: Router) {
  router.beforeEach(async (to, from) => {
    const accessStore = useAccessStore();
    const userStore = useUserStore();
    const authStore = useAuthStore();

    // This route is in coreRoutes for layout purposes, but still requires login.
    if (to.path === CHANGE_PASSWORD_PATH) {
      if (!accessStore.accessToken) {
        return { path: LOGIN_PATH, replace: true };
      }
      const info = userStore.userInfo || (await authStore.fetchUserInfo());
      return info.mustChangePassword
        ? true
        : info.homePath || preferences.app.defaultHomePath;
    }

    if (to.path === PROFILE_PATH) {
      if (!accessStore.accessToken) {
        return { path: LOGIN_PATH, replace: true };
      }
      const info = userStore.userInfo || (await authStore.fetchUserInfo());
      return info.mustChangePassword ? CHANGE_PASSWORD_PATH : true;
    }

    // 基本路由，这些路由不需要进入权限拦截
    if (coreRouteNames.includes(to.name as string)) {
      if (to.path === LOGIN_PATH && accessStore.accessToken) {
        const info = userStore.userInfo || (await authStore.fetchUserInfo());
        if (info.mustChangePassword) {
          return CHANGE_PASSWORD_PATH;
        }
        return decodeURIComponent(
          (to.query?.redirect as string) ||
            userStore.userInfo?.homePath ||
            preferences.app.defaultHomePath,
        );
      }
      return true;
    }

    // accessToken 检查
    if (!accessStore.accessToken) {
      // 明确声明忽略权限访问权限，则可以访问
      if (to.meta.ignoreAccess) {
        return true;
      }

      // 没有访问权限，跳转登录页面
      if (to.fullPath !== LOGIN_PATH) {
        return {
          path: LOGIN_PATH,
          // 如不需要，直接删除 query
          query:
            to.fullPath === preferences.app.defaultHomePath
              ? {}
              : { redirect: encodeURIComponent(to.fullPath) },
          // 携带当前跳转的页面，登录后重新跳转该页面
          replace: true,
        };
      }
      return to;
    }

    const userInfo = userStore.userInfo || (await authStore.fetchUserInfo());
    if (userInfo.mustChangePassword) {
      return CHANGE_PASSWORD_PATH;
    }

    // 是否已经生成过动态路由
    if (accessStore.isAccessChecked) {
      if (to.name === 'FallbackNotFound') return true;
      // Router may retain route records from a previous account in this tab.
      // API permission codes do not grant access to a page without its menu route.
      return hasAccessibleRoute(accessStore.accessRoutes, to.name)
        ? true
        : userInfo.homePath || preferences.app.defaultHomePath;
    }

    // Persisted codes may predate a permission migration; refresh them with routes.
    accessStore.setAccessCodes(await getAccessCodesApi());

    // 生成路由表
    // 当前登录用户拥有的角色标识列表
    const userRoles = userInfo.roles ?? [];

    // 生成菜单和路由
    const { accessibleMenus, accessibleRoutes } = await generateAccess({
      roles: userRoles,
      router,
      // 则会在菜单中显示，但是访问会被重定向到403
      routes: accessRoutes,
    });

    // 保存菜单信息和路由信息
    accessStore.setAccessMenus(accessibleMenus);
    accessStore.setAccessRoutes(accessibleRoutes);
    accessStore.setIsAccessChecked(true);
    const tabbarStore = useTabbarStore();
    const staleTabKeys = tabbarStore.tabs
      .filter(
        (tab) =>
          tab.path !== PROFILE_PATH &&
          !coreRouteNames.includes(tab.name as string) &&
          !hasAccessibleRoute(accessibleRoutes, tab.name),
      )
      .map((tab) => tab.key ?? getTabKey(tab));
    if (staleTabKeys.length > 0) {
      await tabbarStore._bulkCloseByKeys(staleTabKeys);
    }
    const redirectPath = (from.query.redirect ??
      (to.path === preferences.app.defaultHomePath
        ? userInfo.homePath || preferences.app.defaultHomePath
        : to.fullPath)) as string;

    return {
      ...router.resolve(decodeURIComponent(redirectPath)),
      replace: true,
    };
  });
}

/**
 * 项目守卫配置
 * @param router
 */
function createRouterGuard(router: Router) {
  /** 通用 */
  setupCommonGuard(router);
  /** 权限访问 */
  setupAccessGuard(router);
}

export { createRouterGuard };
