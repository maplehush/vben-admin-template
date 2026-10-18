<script setup lang="ts">
import type { Dayjs } from 'dayjs';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LoginLog } from '#/api/system';

import { Page } from '@vben/common-ui';

import { Tag } from 'antdv-next';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getLoginLogs } from '#/api/system';

import { columns, searchSchema } from './data';

type SearchValues = Record<string, unknown> & { occurTime?: [Dayjs, Dayjs] };

const [Grid] = useVbenVxeGrid({
  formOptions: { schema: searchSchema, submitOnChange: true },
  gridOptions: {
    columns,
    height: 'auto',
    rowConfig: { keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, values) => {
          const { occurTime, ...filters } = values as SearchValues;
          return getLoginLogs({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...filters,
            startTime: occurTime?.[0]?.startOf('day').toDate().toISOString(),
            endTime: occurTime?.[1]?.endOf('day').toDate().toISOString(),
          });
        },
      },
    },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LoginLog>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="登录日志">
      <template #result="{ row }">
        <Tag :color="row.success ? 'success' : 'error'">
          {{ row.success ? '成功' : '失败' }}
        </Tag>
      </template>
    </Grid>
  </Page>
</template>
