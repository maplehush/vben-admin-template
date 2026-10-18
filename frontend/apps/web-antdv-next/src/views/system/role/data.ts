import type { VbenFormSchema } from '#/adapter/form';

import { statusFilterOptions, statusFormOptions } from '../common/status';

export function useFormSchema() {
  const schema: VbenFormSchema[] = [
    {
      component: 'Input',
      componentProps: { maxlength: 64 },
      fieldName: 'roleCode',
      label: '角色编码',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'roleName',
      label: '角色名称',
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: { maxlength: 500 },
      fieldName: 'description',
      label: '备注',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: statusFormOptions,
        optionType: 'button',
      },
      fieldName: 'status',
      label: '状态',
    },
  ];
  return schema;
}

export const statusOptions = statusFilterOptions;
export const scopeOptions = [
  { label: '全部数据', value: 'ALL' },
  { label: '本部门及下级', value: 'DEPT_AND_CHILDREN' },
  { label: '本部门', value: 'DEPT' },
  { label: '仅本人', value: 'SELF' },
  { label: '自定义部门', value: 'CUSTOM' },
];
export const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'roleName', label: '角色名称' },
  { component: 'Input', fieldName: 'roleId', label: '角色 ID' },
  {
    component: 'Select',
    componentProps: { allowClear: true, options: statusOptions },
    fieldName: 'status',
    label: '状态',
  },
  { component: 'Input', fieldName: 'description', label: '备注' },
  { component: 'RangePicker', fieldName: 'createTime', label: '创建时间' },
];
export const columns = [
  { field: 'roleCode', title: '角色编码', minWidth: 150 },
  { field: 'roleName', title: '角色名称', minWidth: 150 },
  { field: 'description', title: '备注', minWidth: 200 },
  { field: 'status', title: '状态', width: 120, slots: { default: 'status' } },
  { field: 'dataScopeType', title: '数据范围', minWidth: 140 },
  { field: 'createTime', title: '创建时间', minWidth: 190 },
  {
    field: 'action',
    title: '操作',
    fixed: 'right',
    width: 320,
    slots: { default: 'action' },
  },
];
