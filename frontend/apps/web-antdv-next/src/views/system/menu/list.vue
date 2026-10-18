<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MenuNode } from '#/api/system';

import { ref } from 'vue';

import { useAccess } from '@vben/access';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { Button, message, Modal, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deleteMenu, getMenuTree } from '#/api/system';
import { $t } from '#/locales';

import { columns, typeOptions } from './data';
import MenuForm from './modules/form.vue';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();
const can = (code: string) =>
  hasAccessByCodes([code]) && hasAccessByRoles(['super_admin']);
const protectedKey = (key: string) =>
  key === 'menu.system' ||
  key === 'menu.system.menu' ||
  key === 'menu.system.permission' ||
  key === 'menu.system.dict' ||
  key.startsWith('perm.system.menu.') ||
  key.startsWith('perm.system.permission.');
const coreMenu = (key: string) =>
  ['menu.system', 'menu.system.dict', 'menu.system.menu', 'menu.system.permission'].includes(key);
const tree = ref<MenuNode[]>([]);
const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: MenuForm,
  destroyOnClose: true,
});
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns,
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id' },
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async () => {
          tree.value = await getMenuTree();
          return tree.value;
        },
      },
    },
    toolbarConfig: { custom: true, export: false, refresh: true, zoom: true },
    treeConfig: { parentField: 'parentId', rowField: 'id', transform: false },
  } as VxeTableGridOptions<MenuNode>,
});

function openEditor(row?: MenuNode, child = false) {
  formDrawerApi.setData({ row, child }).open();
}
function remove(row: MenuNode) {
  Modal.confirm({
    title: '删除菜单',
    content: `确定删除 ${$t(row.titleKey)} 吗？有子菜单、接口关联或角色授权时不能删除。`,
    async onOk() {
      await deleteMenu(row.id);
      message.success('删除成功');
      gridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #toolbar-tools>
        <Button
          v-if="can('system:menu:create')"
          type="primary"
          @click="openEditor()"
        >
          <Plus class="size-5" />新增菜单
        </Button>
      </template>
      <template #title="{ row }">
        <div class="flex w-full items-center gap-1">
          <div class="size-5 shrink-0">
            <IconifyIcon
              :icon="
                row.type === 'BUTTON'
                  ? 'carbon:security'
                  : row.routeMeta?.icon || row.icon || 'carbon:circle-dash'
              "
              class="size-full"
            />
          </div>
          <span class="min-w-0 flex-auto truncate">{{ $t(row.titleKey) }}</span>
        </div>
      </template>
      <template #type="{ row }">
        <Tag
          :color="typeOptions.find((item) => item.value === row.type)?.color"
        >
          {{ typeOptions.find((item) => item.value === row.type)?.label }}
        </Tag>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 'ENABLED' ? 'success' : 'error'">
          {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          class="min-w-max"
          :actions="[
            {
              auth: 'system:menu:create',
              ifShow:
                ['CATALOG', 'MENU'].includes(row.type) &&
                hasAccessByRoles(['super_admin']),
              text: '新增下级',
              icon: 'lucide:plus',
              onClick: () => openEditor(row, true),
            },
            {
              auth: protectedKey(row.menuKey)
                ? undefined
                : 'system:menu:update',
              ifShow: hasAccessByRoles(['super_admin']),
              text: '编辑',
              disabled: protectedKey(row.menuKey) && !coreMenu(row.menuKey),
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => openEditor(row),
            },
            {
              auth: protectedKey(row.menuKey)
                ? undefined
                : 'system:menu:delete',
              ifShow: hasAccessByRoles(['super_admin']),
              text: '删除',
              danger: true,
              disabled: protectedKey(row.menuKey),
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => remove(row),
            },
          ]"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.query()" />
  </Page>
</template>
