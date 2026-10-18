import type { ComputedRef } from 'vue';

import type { VbenFormSchema } from '#/adapter/form';
import type { MenuNode } from '#/api/system';

import { menuComponentOptions } from '#/router/routes/menu-components';

import { statusFormOptions } from '../common/status';

export const typeOptions = [
  { color: 'processing', label: '目录', value: 'CATALOG' },
  { color: 'default', label: '菜单', value: 'MENU' },
  { color: 'error', label: '按钮', value: 'BUTTON' },
  { color: 'success', label: '内嵌', value: 'EMBEDDED' },
  { color: 'warning', label: '外链', value: 'LINK' },
];

export function useFormSchema(
  parentTree: ComputedRef<Array<Record<string, unknown>>>,
  onTypeChange: (type: MenuNode['type']) => void,
) {
  const statusOptions = statusFormOptions;
  const schema: VbenFormSchema[] = [
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        onChange: (event: { target: { value: MenuNode['type'] } }) =>
          onTypeChange(event.target.value),
        options: typeOptions,
        optionType: 'button',
      },
      fieldName: 'type',
      formItemClass: 'col-span-2 md:col-span-2',
      label: '类型',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      fieldName: 'menuKey',
      label: '菜单标识',
    },
    {
      component: 'TreeSelect',
      componentProps: () => ({
        allowClear: true,
        class: 'w-full',
        placeholder: '根目录留空',
        treeData: parentTree.value,
      }),
      fieldName: 'parentId',
      help: '目录和菜单都可以作为上级，支持多级嵌套；按钮、内嵌和外链不能作为上级。',
      label: '上级菜单',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255 },
      fieldName: 'titleKey',
      label: '标题',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255 },
      dependencies: {
        show: (values) => values.type !== 'BUTTON' && values.type !== 'LINK',
        triggerFields: ['type'],
      },
      fieldName: 'routePath',
      label: '路由地址',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      dependencies: {
        show: (values) => values.type === 'BUTTON',
        triggerFields: ['type'],
      },
      fieldName: 'authCode',
      label: '权限标识',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 2048 },
      dependencies: {
        show: (values) => ['EMBEDDED', 'LINK'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'linkSrc',
      label: '链接地址',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 128 },
      dependencies: {
        show: (values) => ['CATALOG', 'MENU'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'routeName',
      label: '路由名称',
    },
    {
      component: 'IconPicker',
      componentProps: { prefix: 'mdi' },
      dependencies: {
        show: (values) => values.type !== 'BUTTON',
        triggerFields: ['type'],
      },
      fieldName: 'icon',
      label: '图标',
    },
    {
      component: 'IconPicker',
      componentProps: { prefix: 'mdi' },
      dependencies: {
        show: (values) => values.type !== 'BUTTON',
        triggerFields: ['type'],
      },
      fieldName: 'activeIcon',
      label: '激活图标',
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options: menuComponentOptions,
        showSearch: true,
      },
      dependencies: {
        show: (values) => values.type === 'MENU',
        triggerFields: ['type'],
      },
      fieldName: 'component',
      label: '页面组件',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255 },
      dependencies: {
        show: (values) => ['CATALOG', 'MENU'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'redirectPath',
      label: '重定向路径',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: statusOptions,
        optionType: 'button',
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full' },
      fieldName: 'sortOrder',
      label: '排序',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        class: 'w-full',
        options: [
          { label: '点', value: 'dot' },
          { label: '文字', value: 'normal' },
        ],
      },
      dependencies: {
        show: (values) => values.type !== 'BUTTON',
        triggerFields: ['type'],
      },
      fieldName: 'badgeType',
      label: '徽标类型',
    },
    {
      component: 'Input',
      dependencies: {
        resolve: ({ values }) => ({
          componentProps: { disabled: values.badgeType !== 'normal' },
          show: values.type !== 'BUTTON',
        }),
        triggerFields: ['badgeType', 'type'],
      },
      fieldName: 'badge',
      label: '徽章内容',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        class: 'w-full',
        options: [
          'default',
          'destructive',
          'primary',
          'success',
          'warning',
        ].map((value) => ({ label: value, value })),
      },
      dependencies: {
        show: (values) => values.type !== 'BUTTON',
        triggerFields: ['type'],
      },
      fieldName: 'badgeVariants',
      label: '徽标样式',
    },
    {
      component: 'Divider',
      dependencies: {
        show: (values) => !['BUTTON', 'LINK'].includes(values.type),
        triggerFields: ['type'],
      },
      fieldName: 'advancedDivider',
      formItemClass: 'col-span-2 md:col-span-2 pb-0',
      hideLabel: true,
      renderComponentContent: () => ({ default: () => '其它设置' }),
    },
    ...(
      [
        { fieldName: 'keepAlive', label: '缓存标签页', types: ['MENU'] },
        {
          fieldName: 'affixTab',
          label: '固定在标签页',
          types: ['MENU', 'EMBEDDED'],
        },
        {
          fieldName: 'hideInMenu',
          label: '隐藏菜单',
          types: ['CATALOG', 'MENU', 'EMBEDDED', 'LINK'],
        },
        {
          fieldName: 'hideChildrenInMenu',
          label: '隐藏子菜单',
          types: ['CATALOG', 'MENU'],
        },
        {
          fieldName: 'hideInBreadcrumb',
          label: '在面包屑中隐藏',
          types: ['CATALOG', 'MENU', 'EMBEDDED'],
        },
        {
          fieldName: 'hideInTab',
          label: '在标签栏中隐藏',
          types: ['CATALOG', 'MENU', 'EMBEDDED'],
        },
      ] as const
    ).map(({ fieldName, label, types }) => ({
      component: 'Checkbox' as const,
      dependencies: {
        show: (values: Record<string, unknown>) =>
          (types as readonly string[]).includes(String(values.type)),
        triggerFields: ['type'],
      },
      fieldName,
      renderComponentContent: () => ({ default: () => label }),
    })),
  ];
  return schema;
}

export const columns = [
  {
    align: 'left',
    field: 'titleKey',
    fixed: 'left',
    title: '标题',
    treeNode: true,
    width: 300,
    slots: { default: 'title' },
  },
  {
    align: 'center',
    field: 'type',
    title: '类型',
    width: 110,
    slots: { default: 'type' },
  },
  { align: 'center', field: 'authCode', title: '权限标识', width: 200 },
  { align: 'left', field: 'routePath', title: '路由地址', width: 220 },
  { align: 'left', field: 'component', title: '页面组件', minWidth: 220 },
  { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
  {
    align: 'right',
    field: 'action',
    title: '操作',
    fixed: 'right',
    headerAlign: 'center',
    showOverflow: false,
    width: 280,
    slots: { default: 'action' },
  },
];
