<script setup lang="ts">
import type { MenuNode, PermissionView, Status } from '#/api/system';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createPermission, getMenuTree, updatePermission } from '#/api/system';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits<{ success: [] }>();
const editingId = ref<string>();
const menus = ref<MenuNode[]>([]);
const menuTree = computed(() => {
  const map = (nodes: MenuNode[]): Array<Record<string, unknown>> =>
    nodes
      .filter((node) => node.type !== 'BUTTON')
      .map((node) => ({
        children: map(node.children),
        disabled: node.status !== 'ENABLED',
        label: $t(node.titleKey),
        value: node.id,
      }));
  return map(menus.value);
});
const schema = useFormSchema(menuTree);
const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema,
  showDefaultActions: false,
});
const [Drawer, drawerApi] = useVbenDrawer<null | PermissionView>({
  async onOpenChange(open) {
    if (!open) return;
    const row = drawerApi.getData();
    editingId.value = row?.id;
    formApi.reset();
    formApi.updateSchema([
      { fieldName: 'permissionKey', hide: Boolean(editingId.value) },
      { fieldName: 'permissionCode', hide: Boolean(editingId.value) },
    ]);
    await nextTick();
    try {
      menus.value = await getMenuTree();
      await formApi.setValues(
        row
          ? { ...row, menuId: row.menuId ?? undefined }
          : { sortOrder: 0, status: 'ENABLED' },
      );
    } catch {
      drawerApi.close();
    }
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const permissionKey = String(values.permissionKey ?? '');
    const permissionCode = String(values.permissionCode ?? '');
    const titleKey = String(values.titleKey ?? '').trim();
    const sortOrder = Number(values.sortOrder);
    if (!editingId.value && !/^[A-Za-z0-9_.-]{3,128}$/.test(permissionKey))
      return message.warning('权限标识须为 3–128 位字母、数字或 _ . -');
    if (!editingId.value && !/^[A-Za-z0-9:_-]{3,128}$/.test(permissionCode))
      return message.warning('权限码格式不正确');
    if (!titleKey || !Number.isInteger(sortOrder))
      return message.warning('请填写名称和整数排序值');
    const data = {
      menuId: (values.menuId as string | undefined) ?? null,
      titleKey,
      sortOrder,
      status: (values.status as Status) ?? 'ENABLED',
    };
    drawerApi.lock();
    try {
      await (editingId.value
        ? updatePermission(editingId.value, data)
        : createPermission({ ...data, permissionKey, permissionCode }));
      message.success('保存成功');
      drawerApi.close();
      emit('success');
    } catch {
      /* 全局请求拦截器展示错误 */
    } finally {
      drawerApi.unlock();
    }
  },
});
defineExpose({ drawerApi });
</script>

<template>
  <Drawer
    class="w-full max-w-150"
    :title="editingId ? '编辑接口权限' : '新增接口权限'"
  >
    <Form class="mx-4" />
  </Drawer>
</template>
