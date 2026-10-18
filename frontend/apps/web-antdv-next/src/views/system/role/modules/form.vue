<script setup lang="ts">
import type { MenuNode, Status } from '#/api/system';

import { computed, nextTick, ref } from 'vue';

import { Tree, useVbenDrawer } from '@vben/common-ui';

import { Button, message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createRole, getMenuTree, getRole, getRoleMenus, updateRole } from '#/api/system';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import { enabledMenuIds, sameMenuIds } from '../menu-selection';

const emit = defineEmits<{ success: [] }>();
const editingId = ref<string>();
const menuTree = ref<MenuNode[]>([]);
const menuIds = ref<string[]>([]);
const loading = ref(false);
const visibleMenus = computed(() => {
  const map = (nodes: MenuNode[]): Array<Record<string, unknown>> =>
    nodes.map((node) => ({
      ...node,
      label: $t(node.titleKey),
      disabled: node.status !== 'ENABLED',
      children: map(node.children ?? []),
    }));
  return map(menuTree.value);
});
const schema = useFormSchema();
const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema,
  showDefaultActions: false,
});
const [Drawer, drawerApi] = useVbenDrawer<null | string>({
  async onOpenChange(open) {
    if (!open) return;
    loading.value = true;
    editingId.value = drawerApi.getData() ?? undefined;
    menuIds.value = [];
    menuTree.value = [];
    formApi.reset();
    formApi.updateSchema([
      { fieldName: 'roleCode', hide: Boolean(editingId.value) },
    ]);
    await nextTick();
    try {
      const [values, menus, granted] = await Promise.all([
        editingId.value ? getRole(editingId.value) : Promise.resolve({ status: 'ENABLED' }),
        getMenuTree(),
        editingId.value
          ? getRoleMenus(editingId.value)
          : Promise.resolve({ menuIds: [] as string[] }),
      ]);
      menuTree.value = menus;
      menuIds.value = [...granted.menuIds];
      await formApi.setValues(values);
    } catch {
      drawerApi.close();
    } finally {
      loading.value = false;
    }
  },
  async onConfirm() {
    if (loading.value) return;
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const roleCode = String(values.roleCode ?? '');
    const roleName = String(values.roleName ?? '').trim();
    if (!editingId.value && !/^[A-Za-z0-9_.-]{3,64}$/.test(roleCode))
      return message.warning('角色编码须为 3–64 位字母、数字或 _ . -');
    if (!roleName || roleName.length > 128)
      return message.warning('请填写角色名称（最多 128 字）');
    const selectedMenuIds = [...new Set(menuIds.value)];
    const data = {
      roleName,
      description: String(values.description ?? '').trim() || null,
      status: (values.status as Status) ?? 'ENABLED',
      menuIds: selectedMenuIds,
    };
    drawerApi.lock();
    try {
      await (editingId.value
        ? updateRole(editingId.value, data)
        : createRole({ ...data, roleCode }));
      if (editingId.value) {
        const saved = await getRoleMenus(editingId.value);
        if (!sameMenuIds(selectedMenuIds, saved.menuIds)) {
          message.error('菜单授权保存结果与勾选内容不一致，请重试');
          return;
        }
      }
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
  <Drawer class="w-full max-w-150" :title="editingId ? '编辑角色' : '新增角色'">
    <Form class="mx-4" />
    <div class="mx-4 mt-4">
      <div class="mb-2 text-sm font-medium">菜单与按钮授权</div>
      <div class="mb-2 flex items-center gap-3">
        <Button size="small" @click="menuIds = enabledMenuIds(menuTree)">全选</Button>
        <Button size="small" @click="menuIds = []">清空</Button>
        <span class="text-muted-foreground text-xs">已选 {{ menuIds.length }} 项</span>
      </div>
      <div class="max-h-96 overflow-auto">
        <Tree
          v-model="menuIds"
          :tree-data="visibleMenus"
          multiple
          include-indeterminate
          bordered
          label-field="label"
          value-field="id"
          :default-expanded-level="2"
        />
      </div>
    </div>
  </Drawer>
</template>
