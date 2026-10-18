<script setup lang="ts">
import type { DictItemColor, DictItemView, DictItemWrite, Status } from '#/api/system';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createDictItem, updateDictItem } from '#/api/system';

import { itemSchema } from '../data';

const emit = defineEmits<{ success: [] }>();
const editingId = ref<string>();
const dictId = ref<string>('');
const [Form, formApi] = useVbenForm({ layout: 'vertical', schema: itemSchema, showDefaultActions: false });
const [Drawer, drawerApi] = useVbenDrawer<{ dictId: string; row: DictItemView | null }>({
  async onOpenChange(open) {
    if (!open) return;
    const data = drawerApi.getData();
    if (!data) return;
    dictId.value = data.dictId;
    editingId.value = data.row?.id;
    formApi.reset();
    formApi.updateSchema([{ fieldName: 'itemValue', componentProps: { disabled: !!data.row, maxlength: 128 } }]);
    await nextTick();
    await formApi.setValues(data.row ? { ...data.row } : { itemColor: null, sortOrder: 0, status: 'ENABLED' });
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const data: DictItemWrite = {
      itemValue: String(values.itemValue ?? '').trim(),
      itemLabel: String(values.itemLabel ?? '').trim(),
      sortOrder: Number(values.sortOrder),
      status: (values.status as Status) ?? 'ENABLED',
      remark: String(values.remark ?? '').trim() || null,
      itemColor: values.itemColor ? (values.itemColor as DictItemColor) : null,
    };
    if (!data.itemValue || !data.itemLabel || !Number.isInteger(data.sortOrder))
      return message.warning('请填写字典项值、显示文本和整数排序');
    drawerApi.lock();
    try {
      await (editingId.value
        ? updateDictItem(dictId.value, editingId.value, data)
        : createDictItem(dictId.value, data));
      message.success('保存成功');
      drawerApi.close();
      emit('success');
    } catch { /* 统一请求处理展示错误 */ }
    finally { drawerApi.unlock(); }
  },
});
defineExpose({ drawerApi });
</script>
<template>
  <Drawer class="w-full max-w-150" :title="editingId ? '编辑字典项' : '新增字典项'">
    <Form class="mx-4" />
  </Drawer>
</template>
