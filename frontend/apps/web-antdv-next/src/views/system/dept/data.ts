import type { VbenFormSchema } from '#/adapter/form';

import { statusFormOptions } from '../common/status';

export const columns = [
  { align: 'left', field: 'deptName', title: '部门名称', treeNode: true, width: 180, fixed: 'left' },
  { field: 'deptCode', title: '部门编号', minWidth: 160 },
  { field: 'sortOrder', title: '排序', width: 90 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  { field: 'remark', title: '备注', minWidth: 200 },
  { field: 'createTime', title: '创建时间', minWidth: 190 },
  { field: 'action', title: '操作', fixed: 'right', width: 240, slots: { default: 'action' } },
];

export function useFormSchema(
  loadParentTree: () => Promise<Array<Record<string, unknown>>>,
) {
  const schema: VbenFormSchema[] = [
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        alwaysLoad: true,
        api: loadParentTree,
        class: 'w-full',
        childrenField: 'children',
        labelField: 'deptName',
        placeholder: '留空为根部门',
        valueField: 'id',
      },
      fieldName: 'parentId',
      label: '上级部门',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'deptName',
      label: '部门名称',
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
    {
      component: 'Textarea',
      componentProps: { maxLength: 500, rows: 3, showCount: true },
      fieldName: 'remark',
      label: '备注',
    },
  ];
  return schema;
}
