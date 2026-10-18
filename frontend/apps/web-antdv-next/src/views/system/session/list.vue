<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SessionView } from '#/api/system';

import { Page } from '@vben/common-ui';

import { message, Modal } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getSessions, revokeSession } from '#/api/system';

const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'username', label: '账号' },
];
const columns = [
  { field: 'username', title: '账号', minWidth: 140 },
  { field: 'deviceInfo', title: '设备信息', minWidth: 260 },
  { field: 'ipAddress', title: 'IP 地址', minWidth: 140 },
  { field: 'createTime', title: '登录时间', minWidth: 190 },
  { field: 'expireTime', title: '过期时间', minWidth: 190 },
  {
    field: 'action',
    title: '操作',
    fixed: 'right',
    width: 120,
    slots: { default: 'action' },
  },
];
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: searchSchema, submitOnChange: true },
  gridOptions: {
    columns,
    height: 'auto',
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) =>
          getSessions({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...values,
          }),
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<SessionView>,
});

function forceOffline(row: SessionView) {
  Modal.confirm({
    title: '强制下线',
    content: `确定让 ${row.username} 的这个会话立即下线吗？`,
    async onOk() {
      await revokeSession(row.id);
      message.success('会话已撤销');
      gridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          :actions="[
            {
              auth: 'system:session:revoke',
              text: '强制下线',
              danger: true,
              onClick: () => forceOffline(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
