import type { Ref } from 'vue';

import type { DescriptionsItemType } from '@vben/common-ui';

import type { VbenFormSchema } from '#/adapter/form';
import type { DeptNode, RoleSummary, UserDetail } from '#/api/system';

import { h } from 'vue';

import { Tag } from 'antdv-next';

import { upload_file } from '#/api/core/upload';

import { statusFilterOptions, statusFormOptions } from '../common/status';

export function useDescriptionItems(user?: UserDetail): DescriptionsItemType[] {
  const display = (value: null | string | undefined) => value || '—';
  return [
    { label: '用户名', content: display(user?.username) },
    { label: '用户 ID', content: display(user?.id) },
    { label: '姓名', content: display(user?.realName) },
    { label: '部门', content: display(user?.primaryDeptName) },
    { label: '邮箱', content: display(user?.email) },
    { label: '手机号', content: display(user?.phone) },
    {
      label: '状态',
      content: () =>
        h(
          Tag,
          { color: user?.status === 'ENABLED' ? 'success' : 'error' },
          { default: () => (user?.status === 'ENABLED' ? '已启用' : '已禁用') },
        ),
    },
    { label: '备注', content: display(user?.remark) },
    { label: '创建时间', content: display(user?.createTime) },
    { label: '更新时间', content: display(user?.updateTime) },
  ];
}

export function useFormSchema(depts: Ref<DeptNode[]>, roles: Ref<RoleSummary[]>) {
  const schema: VbenFormSchema[] = [
    {
      component: 'Input',
      componentProps: { maxlength: 64 },
      fieldName: 'username',
      label: '用户名',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'realName',
      label: '姓名',
      rules: 'required',
    },
    {
      component: 'TreeSelect',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        fieldNames: { children: 'children', label: 'deptName', value: 'id' },
        treeData: depts.value,
      }),
      fieldName: 'primaryDeptId',
      label: '部门',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { disabled: true },
      fieldName: 'initialPassword',
      label: '初始密码',
    },
    {
      component: 'Select',
      componentProps: () => ({
        class: 'w-full',
        mode: 'multiple',
        options: roles.value.map((role) => ({
          disabled: role.status !== 'ENABLED',
          label: `${role.roleName} (${role.roleCode})`,
          value: role.id,
        })),
      }),
      fieldName: 'roleIds',
      label: '初始角色',
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
      component: 'Input',
      componentProps: { maxlength: 255 },
      fieldName: 'email',
      label: '邮箱',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 32 },
      fieldName: 'phone',
      label: '手机号',
    },
    {
      component: 'Upload',
      componentProps: {
        accept: '.png,.jpg,.jpeg,.gif,.webp',
        customRequest: upload_file,
        listType: 'picture-card',
        maxCount: 1,
        maxSize: 5,
        onHandleChange: ({ file }) => {
          if (file.status === 'done' && file.response?.url) {
            file.url = file.response.url;
          }
        },
      },
      fieldName: 'avatarFiles',
      label: '头像',
      renderComponentContent: () => ({ default: () => '上传图片' }),
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255 },
      fieldName: 'homePath',
      label: '首页路径',
    },
    {
      component: 'Textarea',
      componentProps: { maxlength: 500 },
      fieldName: 'remark',
      formItemClass: 'col-span-2',
      label: '备注',
    },
  ];
  return schema;
}

export const statusOptions = statusFilterOptions;
export const searchSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'username', label: '用户名' },
  { component: 'Input', fieldName: 'userId', label: '用户 ID' },
  {
    component: 'Select',
    componentProps: { allowClear: true, options: statusOptions },
    fieldName: 'status',
    label: '状态',
  },
  { component: 'Input', fieldName: 'remark', label: '备注' },
  { component: 'RangePicker', fieldName: 'createTime', label: '创建时间' },
];
export const columns = [
  { field: 'username', title: '用户名', minWidth: 130 },
  { field: 'id', title: '用户 ID', minWidth: 100 },
  { field: 'realName', title: '姓名', minWidth: 120 },
  { field: 'primaryDeptName', title: '部门', minWidth: 130 },
  { field: 'email', title: '邮箱', minWidth: 180 },
  { field: 'phone', title: '手机号', minWidth: 130 },
  { field: 'status', title: '状态', width: 120, slots: { default: 'status' } },
  { field: 'remark', title: '备注', minWidth: 170 },
  { field: 'createTime', title: '创建时间', minWidth: 190 },
  {
    field: 'action',
    title: '操作',
    fixed: 'right',
    width: 260,
    slots: { default: 'action' },
  },
];
