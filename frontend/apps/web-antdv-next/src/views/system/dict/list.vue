<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DictQuery, DictView } from '#/api/system';

import { useAccess } from '@vben/access';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deleteDict, getDicts } from '#/api/system';

import { dictColumns, searchSchema } from './data';
import DictForm from './modules/dict-form.vue';
import DictItems from './modules/items.vue';

const { hasAccessByCodes } = useAccess();
const can = (code: string) => hasAccessByCodes([code]);
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: searchSchema },
  gridOptions: {
    columns: dictColumns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSizes: [10, 20, 30, 50, 100] },
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) => {
          return getDicts({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...(values as Partial<DictQuery>),
          });
        },
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<DictView>,
});

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: DictForm,
  destroyOnClose: true,
});
const [ItemsDrawer, itemsDrawerApi] = useVbenDrawer({
  connectedComponent: DictItems,
  destroyOnClose: true,
});

function openEditor(row?: DictView) {
  formDrawerApi.setData(row ?? null).open();
}

function openItems(row: DictView) {
  itemsDrawerApi.setData(row).open();
}

function removeDict(row: DictView) {
  Modal.confirm({
    title: '删除字典',
    content: `确定删除 ${row.dictName} 吗？字典项需先删除。`,
    async onOk() {
      await deleteDict(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="字典列表">
      <template #toolbar-tools>
        <Button
          v-if="can('system:dict:create')"
          type="primary"
          @click="openEditor()"
        >
          <Plus class="size-5" />新增字典
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 'ENABLED' ? 'success' : 'error'">
          {{ row.status === 'ENABLED' ? '已启用' : '已禁用' }}
        </Tag>
      </template>
      <template #builtIn="{ row }">
        {{ row.builtIn ? '系统内置' : '自定义' }}
      </template>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          :actions="[
            {
              auth: 'system:dict:view',
              text: '字典配置',
              onClick: () => openItems(row),
            },
            {
              auth: 'system:dict:update',
              text: '编辑',
              disabled: row.builtIn,
              tooltip: row.builtIn ? '系统内置字典不可修改' : undefined,
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => openEditor(row),
            },
            {
              auth: 'system:dict:delete',
              text: '删除',
              danger: true,
              disabled: row.builtIn,
              tooltip: row.builtIn ? '系统内置字典不可删除' : undefined,
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => removeDict(row),
            },
          ]"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.reload()" />
    <ItemsDrawer />
  </Page>
</template>
