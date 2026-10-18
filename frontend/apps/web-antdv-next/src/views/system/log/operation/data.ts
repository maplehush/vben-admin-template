import type { VbenFormSchema } from '#/adapter/form';

export const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'username', label: '操作人' },
  { component: 'Input', fieldName: 'actionCode', label: '操作码' },
  { component: 'RangePicker', fieldName: 'occurTime', label: '发生时间' },
];

export const columns = [
  { field: 'username', title: '操作人', minWidth: 150 },
  { field: 'actionCode', title: '操作码', minWidth: 240 },
  { field: 'targetType', title: '对象类型', minWidth: 130 },
  { field: 'targetId', title: '对象 ID', minWidth: 140 },
  { field: 'success', title: '结果', width: 100, slots: { default: 'result' } },
  { field: 'ipAddress', title: 'IP 地址', minWidth: 150 },
  { field: 'occurTime', title: '发生时间', minWidth: 190 },
];
