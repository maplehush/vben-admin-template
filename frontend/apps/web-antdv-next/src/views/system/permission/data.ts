import type { ComputedRef } from 'vue';

import type { VbenFormSchema } from '#/adapter/form';

import { statusFormOptions } from '../common/status';

export function useFormSchema(
  menuTree: ComputedRef<Array<Record<string, unknown>>>,
) {
  const schema: VbenFormSchema[] = [
    {
      component: 'TreeSelect',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        placeholder: '可不关联菜单',
        treeData: menuTree.value,
      }),
      fieldName: 'menuId',
      label: '所属菜单',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'permissionKey',
      label: '权限标识',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'permissionCode',
      label: '权限码',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255 },
      fieldName: 'titleKey',
      label: '名称',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full' },
      fieldName: 'sortOrder',
      label: '排序',
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

export const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'permissionCode', label: '权限码' },
];
export const columns = [
  { field: 'permissionCode', title: '权限码', minWidth: 220 },
  { field: 'titleKey', title: '名称', minWidth: 180 },
  { field: 'permissionKey', title: '标识', minWidth: 230 },
  { field: 'menuId', title: '所属菜单 ID', minWidth: 130 },
  { field: 'sortOrder', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 90, slots: { default: 'status' } },
  {
    field: 'action',
    title: '操作',
    fixed: 'right',
    width: 160,
    slots: { default: 'action' },
  },
];
