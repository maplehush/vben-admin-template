<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DeptNode } from '#/api/system';

import { useAccess } from '@vben/access';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deleteDept, getDeptTree } from '#/api/system';

import { columns } from './data';
import DeptForm from './modules/form.vue';

const { hasAccessByCodes } = useAccess();
const can = (code: string) => hasAccessByCodes([code]);
const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: DeptForm,
  destroyOnClose: true,
});
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns,
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id' },
    pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: getDeptTree } },
    toolbarConfig: { custom: true, export: false, refresh: true, zoom: true },
    treeConfig: { parentField: 'parentId', rowField: 'id', transform: false },
  } as VxeTableGridOptions<DeptNode>,
});

function openEditor(mode: 'child' | 'edit' | 'root', row?: DeptNode) {
  formDrawerApi.setData({ mode, row }).open();
}

function removeDept(row: DeptNode) {
  Modal.confirm({
    title: '删除部门',
    content: `确定删除 ${row.deptName} 吗？存在子部门、用户或角色数据范围引用时将无法删除。`,
    async onOk() {
      await deleteDept(row.id);
      message.success('删除成功');
      await gridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="部门列表">
      <template #toolbar-tools>
        <Button
          v-if="can('system:dept:create')"
          type="primary"
          @click="openEditor('root')"
        >
          <Plus class="size-5" />新增部门
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
              auth: 'system:dept:create',
              text: '新增下级',
              icon: 'lucide:plus',
              onClick: () => openEditor('child', row),
            },
            {
              auth: 'system:dept:update',
              text: '编辑',
              onClick: () => openEditor('edit', row),
            },
            {
              auth: 'system:dept:delete',
              text: '删除',
              danger: true,
              disabled: row.status === 'ENABLED' || Boolean(row.children?.length),
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => removeDept(row),
            },
          ]"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.query()" />
  </Page>
</template>
