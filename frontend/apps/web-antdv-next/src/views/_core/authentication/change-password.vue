<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, reactive, ref } from 'vue';

import { useVbenForm, VbenButton, z } from '@vben/common-ui';

import { message } from 'antdv-next';

import { changeOwnPasswordApi } from '#/api/core/user';
import { useAuthStore } from '#/store';

defineOptions({ name: 'ChangePassword' });

const authStore = useAuthStore();
const loading = ref(false);
const formSchema = computed((): VbenFormSchema[] => [
  {
    component: 'VbenInputPassword',
    componentProps: { placeholder: '请输入当前密码' },
    fieldName: 'oldPassword',
    label: '当前密码',
    rules: z.string().min(1, { message: '请输入当前密码' }),
  },
  {
    component: 'VbenInputPassword',
    componentProps: { placeholder: '至少 8 位' },
    fieldName: 'newPassword',
    label: '新密码',
    rules: z.string().min(8, { message: '新密码至少 8 位' }).max(128),
  },
  {
    component: 'VbenInputPassword',
    componentProps: { placeholder: '请再次输入新密码' },
    fieldName: 'confirmPassword',
    label: '确认新密码',
    dependencies: {
      rules(values) {
        return z.string().refine((value) => value === values.newPassword, {
          message: '两次输入的新密码不一致',
        });
      },
      triggerFields: ['newPassword'],
    },
  },
]);

const [Form, formApi] = useVbenForm(
  reactive({
    commonConfig: { hideLabel: false },
    schema: formSchema,
    showDefaultActions: false,
  }),
);

async function handleSubmit() {
  const { valid } = await formApi.validate();
  if (!valid) return;
  const values = await formApi.getValues();
  loading.value = true;
  try {
    await changeOwnPasswordApi({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    });
    message.success('密码修改成功，请重新登录');
    await authStore.logout(false);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div @keydown.enter.prevent="handleSubmit">
    <h1 class="mb-2 text-2xl font-semibold">请先修改初始密码</h1>
    <p class="text-muted-foreground mb-8 text-sm">
      为保护账号安全，修改密码后需要重新登录。
    </p>
    <Form />
    <VbenButton class="mt-6 w-full" :loading="loading" @click="handleSubmit">
      确认修改
    </VbenButton>
  </div>
</template>
