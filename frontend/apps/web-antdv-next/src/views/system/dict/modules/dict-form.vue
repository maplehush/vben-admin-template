<script setup lang="ts">
import type { DictView, DictWrite, Status } from '#/api/system';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import { createDict, updateDict } from '#/api/system';

import { dictSchema } from '../data';

const emit = defineEmits<{ success: [] }>();
const editingId = ref<string>();
const [Form, formApi] = useVbenForm({ layout: 'vertical', schema: dictSchema, showDefaultActions: false });
const [Drawer, drawerApi] = useVbenDrawer<DictView | null>({
  async onOpenChange(open) {
    if (!open) return;
    const row = drawerApi.getData();
    editingId.value = row?.id;
    formApi.reset();
    formApi.updateSchema([{ fieldName: 'dictCode', componentProps: { disabled: !!row, maxlength: 64 } }]);
    await nextTick();
    await formApi.setValues(row ? { ...row } : { status: 'ENABLED' });
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    const data: DictWrite = {
      dictCode: String(values.dictCode ?? '').trim(),
      dictName: String(values.dictName ?? '').trim(),
      status: (values.status as Status) ?? 'ENABLED',
      remark: String(values.remark ?? '').trim() || null,
    };
    if (!/^[a-z][a-z0-9_]{1,63}$/.test(data.dictCode))
      return message.warning('字典编码须为小写字母开头的 2–64 位字母、数字或下划线');
    drawerApi.lock();
    try {
      await (editingId.value ? updateDict(editingId.value, data) : createDict(data));
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
  <Drawer class="w-full max-w-150" :title="editingId ? '编辑字典' : '新增字典'">
    <Form class="mx-4" />
  </Drawer>
</template>
