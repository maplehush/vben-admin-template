import type { VbenFormSchema } from '#/adapter/form';

export const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'username', label: '账号' },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: [
        { label: '成功', value: true },
        { label: '失败', value: false },
      ],
    },
    fieldName: 'success',
    label: '结果',
  },
  { component: 'RangePicker', fieldName: 'occurTime', label: '发生时间' },
];

export const columns = [
  { field: 'username', title: '账号', minWidth: 150 },
  { field: 'success', title: '结果', width: 100, slots: { default: 'result' } },
  { field: 'failureReason', title: '失败原因', minWidth: 140 },
  { field: 'ipAddress', title: 'IP 地址', minWidth: 150 },
  { field: 'userAgent', title: '设备信息', minWidth: 260 },
  { field: 'occurTime', title: '发生时间', minWidth: 190 },
];
