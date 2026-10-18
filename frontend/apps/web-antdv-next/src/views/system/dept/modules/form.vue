<script setup lang="ts">
import type { DeptNode, Status } from '#/api/system';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createDept, getDeptTree, updateDept } from '#/api/system';

import { useFormSchema } from '../data';

type DeptDrawerData = { mode: 'child' | 'edit' | 'root'; row?: DeptNode };

const emit = defineEmits<{ success: [] }>();
const editingId = ref<string>();
async function loadParentTree(): Promise<Array<Record<string, unknown>>> {
  const tree = await getDeptTree();
  const blocked = new Set<string>();
  const collect = (node: DeptNode) => {
    blocked.add(node.id);
    node.children.forEach(collect);
  };
  const find = (nodes: DeptNode[]) => {
    for (const node of nodes) {
      if (node.id === editingId.value) collect(node);
      else find(node.children);
    }
  };
  find(tree);
  const map = (nodes: DeptNode[]): Array<Record<string, unknown>> =>
    nodes.map((node) => ({
      ...node,
      children: map(node.children),
      disabled: blocked.has(node.id),
    }));
  return map(tree);
}
const schema = useFormSchema(loadParentTree);
const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema,
  showDefaultActions: false,
});
const [Drawer, drawerApi] = useVbenDrawer<DeptDrawerData>({
  async onOpenChange(open) {
    if (!open) return;
    const data = drawerApi.getData();
    if (!data) return;
    editingId.value = data.mode === 'edit' ? data.row?.id : undefined;
    formApi.reset();
    await nextTick();
    try {
      await formApi.setValues(
        data.mode === 'edit' && data.row
          ? { ...data.row, parentId: data.row.parentId ?? undefined }
          : {
              parentId: data.mode === 'child' ? data.row?.id : undefined,
              sortOrder: 0,
              status: 'ENABLED',
            },
      );
    } catch {
      drawerApi.close();
    }
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const deptName = String(values.deptName ?? '').trim();
    const sortOrder = Number(values.sortOrder);
    const remark = String(values.remark ?? '').trim();
    if (!deptName || deptName.length > 128)
      return message.warning('请填写部门名称（最多 128 字）');
    if (!Number.isInteger(sortOrder)) return message.warning('排序值须为整数');
    if (remark.length > 500) return message.warning('备注最多 500 字');
    const data = {
      parentId: (values.parentId as string | undefined) ?? null,
      deptName,
      sortOrder,
      status: (values.status as Status) ?? 'ENABLED',
      remark: remark || null,
    };
    drawerApi.lock();
    try {
      await (editingId.value
        ? updateDept(editingId.value, data)
        : createDept(data));
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
  <Drawer class="w-full max-w-150" :title="editingId ? '编辑部门' : '新增部门'">
    <Form class="mx-4" />
  </Drawer>
</template>
