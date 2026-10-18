<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { DictItemQuery, DictItemView, DictView } from '#/api/system';

import { ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deleteDictItem, getDictItems } from '#/api/system';

import { itemColorOptions, itemColumns, itemSearchSchema } from '../data';
import ItemForm from './item-form.vue';

const { hasAccessByCodes } = useAccess();
const can = (code: string) => hasAccessByCodes([code]);
const dict = ref<DictView>();

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: itemSearchSchema },
  gridOptions: {
    columns: itemColumns,
    height: 500,
    pagerConfig: { enabled: true, pageSizes: [10, 20, 30, 50, 100] },
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      autoLoad: false,
      ajax: {
        query: async ({ page }, values) => {
          if (!dict.value) return { items: [], total: 0 };
          return getDictItems(dict.value.id, {
            page: page.currentPage,
            pageSize: page.pageSize,
            ...(values as Partial<DictItemQuery>),
          });
        },
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<DictItemView>,
});

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: ItemForm,
  destroyOnClose: true,
});
const [Drawer, drawerApi] = useVbenDrawer<DictView>({
  onOpenChange(open) {
    if (!open) return;
    dict.value = drawerApi.getData();
  },
  onOpened() {
    gridApi.query();
  },
});

function openEditor(row?: DictItemView) {
  if (!dict.value) return;
  formDrawerApi.setData({ dictId: dict.value.id, row: row ?? null }).open();
}

function removeItem(row: DictItemView) {
  if (!dict.value) return;
  const dictId = dict.value.id;
  Modal.confirm({
    title: '删除字典项',
    content: `确定删除 ${row.itemLabel} 吗？`,
    async onOk() {
      await deleteDictItem(dictId, row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}
</script>

<template>
  <Drawer :footer="false" class="w-full max-w-240" :title="`${dict?.dictName ?? ''} · 字典项`">
    <Grid table-title="字典项列表">
      <template #toolbar-tools>
        <Button
          v-if="dict && can('system:dict:create')"
          type="primary"
          :disabled="dict.builtIn"
          :title="dict.builtIn ? '系统内置字典不可新增字典项' : undefined"
          @click="openEditor()"
        >
          <Plus class="size-5" />新增字典项
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 'ENABLED' ? 'success' : 'error'">
          {{ row.status === 'ENABLED' ? '已启用' : '已禁用' }}
        </Tag>
      </template>
      <template #itemColor="{ row }">
        <span v-if="row.itemColor?.startsWith('#')" class="inline-flex items-center gap-2">
          <span class="size-4 rounded-full border border-black/10" :style="{ backgroundColor: row.itemColor }"></span>
          {{ row.itemColor }}
        </span>
        <Tag v-else-if="row.itemColor" :color="row.itemColor">
          {{ itemColorOptions.find((option) => option.value === row.itemColor)?.label }}
        </Tag>
        <span v-else>—</span>
      </template>
      <template #action="{ row }">
        <VbenTableAction
          align="center"
          :actions="[
            {
              auth: 'system:dict:update',
              text: '编辑',
              disabled: dict?.builtIn,
              tooltip: dict?.builtIn ? '系统内置字典项不可修改' : undefined,
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => openEditor(row),
            },
            {
              auth: 'system:dict:delete',
              text: '删除',
              danger: true,
              disabled: dict?.builtIn,
              tooltip: dict?.builtIn ? '系统内置字典项不可删除' : undefined,
              class: 'disabled:pointer-events-auto disabled:cursor-not-allowed disabled:opacity-100 disabled:text-[#bfbfbf]',
              onClick: () => removeItem(row),
            },
          ]"
        />
      </template>
    </Grid>
    <FormDrawer @success="gridApi.reload()" />
  </Drawer>
</template>
