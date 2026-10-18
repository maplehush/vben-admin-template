<script setup lang="ts">
import type { MenuNode, MenuRouteMeta, MenuWrite, Status } from '#/api/system';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { breakpointsTailwind, useBreakpoints } from '@vueuse/core';
import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createMenu, getMenuTree, updateMenu } from '#/api/system';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

type MenuDrawerData = { child?: boolean; row?: MenuNode };

const emit = defineEmits<{ success: [] }>();
const breakpoints = useBreakpoints(breakpointsTailwind);
const isHorizontal = computed(() => breakpoints.greaterOrEqual('md').value);
const editingId = ref<string>();
const editingRow = ref<MenuNode>();
const coreMenuKeys = new Set([
  'menu.system',
  'menu.system.dict',
  'menu.system.menu',
  'menu.system.permission',
]);
const coreMenuFields = new Set(['icon', 'sortOrder', 'titleKey']);
const selectedType = ref<MenuNode['type']>('CATALOG');
const tree = ref<MenuNode[]>([]);
const parentTree = computed(() => {
  const blocked = new Set<string>();
  const collect = (node: MenuNode) => {
    blocked.add(node.id);
    node.children.forEach(collect);
  };
  const find = (nodes: MenuNode[]) =>
    nodes.forEach((node) => {
      if (node.id === editingId.value) collect(node);
      else find(node.children);
    });
  find(tree.value);
  const map = (nodes: MenuNode[]): Array<Record<string, unknown>> =>
    nodes.map((node) => ({
      children: map(node.children),
      disabled:
        node.status !== 'ENABLED' ||
        blocked.has(node.id) ||
        node.type === 'BUTTON' ||
        node.type === 'EMBEDDED' ||
        node.type === 'LINK',
      label: $t(node.titleKey),
      value: node.id,
    }));
  return map(tree.value);
});
const schema = useFormSchema(parentTree, (type) => {
  selectedType.value = type;
});
const [Form, formApi] = useVbenForm({
  commonConfig: {
    colon: true,
    formItemClass: 'col-span-2 md:col-span-1',
  },
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-4',
});
const [Drawer, drawerApi] = useVbenDrawer<MenuDrawerData>({
  async onOpenChange(open) {
    if (!open) return;
    const { row, child } = drawerApi.getData() ?? {};
    editingId.value = child ? undefined : row?.id;
    editingRow.value = child ? undefined : row;
    const editingCoreMenu = Boolean(
      editingRow.value && coreMenuKeys.has(editingRow.value.menuKey),
    );
    let type: MenuNode['type'] = row?.type ?? 'CATALOG';
    if (child) type = row?.type === 'CATALOG' ? 'MENU' : 'BUTTON';
    selectedType.value = type;
    formApi.reset();
    formApi.updateSchema(
      schema.map(({ fieldName }) => ({
        fieldName,
        hide: editingCoreMenu
          ? !coreMenuFields.has(fieldName ?? '')
          : ['menuKey', 'type'].includes(fieldName ?? '') &&
            Boolean(editingId.value),
      })),
    );
    await nextTick();
    try {
      tree.value = await getMenuTree();
      await formApi.setValues(
        row && !child
          ? {
              ...row,
              parentId: row.parentId ?? undefined,
              component: row.component ?? undefined,
              activeIcon: row.routeMeta?.activeIcon ?? '',
              badgeType: row.routeMeta?.badgeType,
              badge: row.routeMeta?.badge ?? '',
              badgeVariants: row.routeMeta?.badgeVariants,
              keepAlive: row.routeMeta?.keepAlive ?? false,
              affixTab: row.routeMeta?.affixTab ?? false,
              hideInMenu: row.routeMeta?.hideInMenu ?? false,
              hideChildrenInMenu: row.routeMeta?.hideChildrenInMenu ?? false,
              hideInBreadcrumb: row.routeMeta?.hideInBreadcrumb ?? false,
              hideInTab: row.routeMeta?.hideInTab ?? false,
              linkSrc: row.routeMeta?.iframeSrc ?? row.routeMeta?.link ?? '',
            }
          : {
              parentId: child ? row?.id : undefined,
              type: selectedType.value,
              sortOrder: 0,
              status: 'ENABLED',
              badgeType: 'dot',
            },
      );
    } catch {
      drawerApi.close();
    }
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const coreRow = editingRow.value;
    if (coreRow && coreMenuKeys.has(coreRow.menuKey)) {
      const titleKey = String(values.titleKey ?? '').trim();
      const sortOrder = Number(values.sortOrder);
      if (!titleKey) return message.warning('请填写标题');
      if (!Number.isInteger(sortOrder))
        return message.warning('排序值须为整数');
      drawerApi.lock();
      try {
        await updateMenu(coreRow.id, {
          parentId: coreRow.parentId,
          routeName: coreRow.routeName,
          routePath: coreRow.routePath,
          component: coreRow.component,
          redirectPath: coreRow.redirectPath,
          titleKey,
          icon: String(values.icon ?? '').trim() || null,
          sortOrder,
          routeMeta: coreRow.routeMeta,
          status: coreRow.status,
        });
        message.success('保存成功');
        drawerApi.close();
        emit('success');
      } catch {
        /* 全局请求拦截器展示错误 */
      } finally {
        drawerApi.unlock();
      }
      return;
    }
    const type = editingId.value
      ? selectedType.value
      : (values.type as MenuNode['type']);
    const menuKey = String(values.menuKey ?? '');
    const authCode = String(values.authCode ?? '').trim();
    const titleKey = String(values.titleKey ?? '').trim();
    let routeName = String(values.routeName ?? '').trim();
    let routePath = String(values.routePath ?? '').trim();
    const component = String(values.component ?? '');
    const linkSrc = String(values.linkSrc ?? '').trim();
    const sortOrder = Number(values.sortOrder);
    if (!editingId.value && !/^[A-Za-z0-9_.-]{3,128}$/.test(menuKey))
      return message.warning('菜单标识须为 3–128 位字母、数字或 _ . -');
    if (['EMBEDDED', 'LINK'].includes(type)) {
      if (editingRow.value) {
        routeName = editingRow.value.routeName ?? '';
        routePath = editingRow.value.routePath ?? '';
      } else {
        const routeId = crypto.randomUUID().replaceAll('-', '');
        routeName = `${type === 'LINK' ? 'External' : 'Embedded'}_${routeId}`;
        if (type === 'LINK') routePath = `/external/${routeId}`;
      }
    }
    if (!titleKey) return message.warning('请填写标题');
    if (!Number.isInteger(sortOrder)) return message.warning('排序值须为整数');
    if (type !== 'BUTTON' && (!routeName || !routePath.startsWith('/')))
      return message.warning('请填写路由名称和以 / 开头的路径');
    if (type === 'MENU' && !component)
      return message.warning('请选择已打包的页面组件');
    if (type === 'EMBEDDED' || type === 'LINK') {
      try {
        const url = new URL(linkSrc);
        if (
          !['http:', 'https:'].includes(url.protocol) ||
          !url.hostname ||
          url.username ||
          url.password
        )
          return message.warning('请填写有效的 HTTP 或 HTTPS 链接地址');
      } catch {
        return message.warning('请填写有效的 HTTP 或 HTTPS 链接地址');
      }
    }
    if (type === 'BUTTON' && !editingId.value && !authCode)
      return message.warning('请填写按钮权限标识');
    const badgeType = values.badgeType as MenuRouteMeta['badgeType'];
    const badge = String(values.badge ?? '').trim();
    if (badgeType === 'normal' && !badge)
      return message.warning('文字徽标需要填写徽章内容');
    const previousMeta = editingRow.value?.routeMeta;
    const routeMeta: MenuRouteMeta = {
      ...(previousMeta?.title && { title: previousMeta.title }),
      ...(previousMeta?.icon && { icon: previousMeta.icon }),
      ...(previousMeta?.order !== undefined && { order: previousMeta.order }),
      ...(type === 'EMBEDDED' && { iframeSrc: linkSrc }),
      ...(type === 'LINK' && { link: linkSrc }),
      ...(type !== 'BUTTON' && {
        ...(String(values.activeIcon ?? '').trim() && {
          activeIcon: String(values.activeIcon).trim(),
        }),
        ...(badgeType && { badgeType }),
        ...(badgeType === 'normal' && { badge }),
        ...(values.badgeVariants && {
          badgeVariants: values.badgeVariants as MenuRouteMeta['badgeVariants'],
        }),
        hideInMenu: Boolean(values.hideInMenu),
      }),
      ...(type === 'MENU' && { keepAlive: Boolean(values.keepAlive) }),
      ...(['EMBEDDED', 'MENU'].includes(type) && {
        affixTab: Boolean(values.affixTab),
      }),
      ...(['CATALOG', 'MENU'].includes(type) && {
        hideChildrenInMenu: Boolean(values.hideChildrenInMenu),
      }),
      ...(['CATALOG', 'EMBEDDED', 'MENU'].includes(type) && {
        hideInBreadcrumb: Boolean(values.hideInBreadcrumb),
        hideInTab: Boolean(values.hideInTab),
      }),
    };
    const isRoute = type !== 'BUTTON';
    const data: MenuWrite = {
      parentId: (values.parentId as string | undefined) ?? null,
      routeName: isRoute ? routeName || null : null,
      routePath: isRoute ? routePath || null : null,
      component: type === 'MENU' ? component || null : null,
      redirectPath: isRoute
        ? String(values.redirectPath ?? '').trim() || null
        : null,
      titleKey,
      icon: String(values.icon ?? '').trim() || null,
      sortOrder,
      routeMeta: Object.keys(routeMeta).length > 0 ? routeMeta : null,
      status: (values.status as Status) ?? 'ENABLED',
    };
    drawerApi.lock();
    try {
      await (editingId.value
        ? updateMenu(editingId.value, data)
        : createMenu({
            ...data,
            type,
            menuKey,
            authCode: type === 'BUTTON' ? authCode : null,
          }));
      message.success('保存成功');
      drawerApi.close();
      emit('success');
    } catch {
      /* 全局请求拦截器展示错误 */
    } finally {
      drawerApi.unlock();
    }
  },
});
defineExpose({ drawerApi });
</script>

<template>
  <Drawer class="w-full max-w-200" :title="editingId ? '编辑菜单' : '新增菜单'">
    <Form class="mx-4" :layout="isHorizontal ? 'horizontal' : 'vertical'" />
  </Drawer>
</template>
