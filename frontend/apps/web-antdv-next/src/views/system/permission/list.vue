<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { PermissionView } from '#/api/system';

import { useAccess } from '@vben/access';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deletePermission, getPermissions } from '#/api/system';

import { columns, searchSchema } from './data';
import PermissionForm from './modules/form.vue';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();
const can = (code: string) =>
  hasAccessByCodes([code]) && hasAccessByRoles(['super_admin']);
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: searchSchema, submitOnChange: true },
  gridOptions: {
    columns,
    height: 'auto',
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) =>
          getPermissions({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...values,
          }),
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<PermissionView>,
});

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: PermissionForm,
  destroyOnClose: true,
});
function openEditor(row?: PermissionView) {
  formDrawerApi.setData(row ?? null).open();
}

function remove(row: PermissionView) {
  Modal.confirm({
    title: '删除接口权限',
    content: `确定删除 ${row.permissionCode} 吗？`,
    async onOk() {
      await deletePermission(row.id);
      message.success('删除成功');
      gridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="接口权限列表">
      <template #toolbar-tools>
        <Button
          v-if="can('system:permission:create')"
          type="primary"
          @click="openEditor()"
        >
          <Plus class="size-5" />新增接口权限
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 'ENABLED' ? 'success' : 'error'">
          {{ row.status === 'ENABLED' ? '已启用' : '已禁用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          :actions="[
            {
              auth: 'system:permission:update',
              ifShow:
                !row.permissionKey.startsWith('perm.system.') &&
                hasAccessByRoles(['super_admin']),
              text: '编辑',
              onClick: () => openEditor(row),
            },
            {
              auth: 'system:permission:delete',
              ifShow:
                !row.permissionKey.startsWith('perm.system.') &&
                hasAccessByRoles(['super_admin']),
              text: '删除',
              danger: true,
              onClick: () => remove(row),
            },
          ]"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.query()" />
  </Page>
</template>
