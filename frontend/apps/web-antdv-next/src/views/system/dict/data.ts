import type { VbenFormSchema } from '#/adapter/form';

import { statusFilterOptions, statusFormOptions } from '../common/status';

export const itemColorOptions = [
  { label: '绿色（成功）', value: 'success' },
  { label: '橙色（警告）', value: 'warning' },
  { label: '红色（错误）', value: 'error' },
  { label: '蓝色（处理中）', value: 'processing' },
] as const;

export const searchSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入字典编码' },
    fieldName: 'dictCode',
    label: '字典编码',
  },
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入字典名称' },
    fieldName: 'dictName',
    label: '字典名称',
  },
  {
    component: 'Select',
    componentProps: { allowClear: true, options: statusFilterOptions },
    fieldName: 'status',
    label: '状态',
  },
];

export const itemSearchSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入显示文本' },
    fieldName: 'itemLabel',
    label: '显示文本',
  },
  {
    component: 'Select',
    componentProps: { allowClear: true, options: statusFilterOptions },
    fieldName: 'status',
    label: '状态',
  },
];

export const dictColumns = [
  { field: 'dictCode', title: '字典编码', minWidth: 180 },
  { field: 'dictName', title: '字典名称', minWidth: 160 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  { field: 'builtIn', title: '类型', width: 100, slots: { default: 'builtIn' } },
  { field: 'remark', title: '备注', minWidth: 180 },
  { field: 'createTime', title: '创建时间', minWidth: 190 },
  { field: 'action', title: '操作', fixed: 'right', width: 230, slots: { default: 'action' } },
];

export const itemColumns = [
  { field: 'itemValue', title: '字典项值', minWidth: 150 },
  { field: 'itemLabel', title: '显示文本', minWidth: 150 },
  { field: 'sortOrder', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  { field: 'itemColor', title: '字典颜色', width: 140, slots: { default: 'itemColor' } },
  { field: 'remark', title: '备注', minWidth: 180 },
  { field: 'action', title: '操作', fixed: 'right', width: 160, slots: { default: 'action' } },
];

export const dictSchema: VbenFormSchema[] = [
  { component: 'Input', componentProps: { maxlength: 64 }, fieldName: 'dictCode', label: '字典编码', rules: 'required' },
  { component: 'Input', componentProps: { maxlength: 128 }, fieldName: 'dictName', label: '字典名称', rules: 'required' },
  {
    component: 'RadioGroup', componentProps: {
      buttonStyle: 'solid', optionType: 'button', options: statusFormOptions,
    }, fieldName: 'status', label: '状态',
  },
  { component: 'Textarea', componentProps: { maxlength: 500 }, fieldName: 'remark', label: '备注' },
];

export const itemSchema: VbenFormSchema[] = [
  { component: 'Input', componentProps: { maxlength: 128 }, fieldName: 'itemValue', label: '字典项值', rules: 'required' },
  { component: 'Input', componentProps: { maxlength: 128 }, fieldName: 'itemLabel', label: '显示文本', rules: 'required' },
  { component: 'InputNumber', componentProps: { class: 'w-full' }, fieldName: 'sortOrder', label: '排序', rules: 'required' },
  {
    component: 'RadioGroup', componentProps: {
      buttonStyle: 'solid', optionType: 'button', options: statusFormOptions,
    }, fieldName: 'status', label: '状态',
  },
  {
    component: 'ColorPalettePicker',
    fieldName: 'itemColor',
    label: '字典颜色',
  },
  { component: 'Textarea', componentProps: { maxlength: 500 }, fieldName: 'remark', label: '备注' },
];
