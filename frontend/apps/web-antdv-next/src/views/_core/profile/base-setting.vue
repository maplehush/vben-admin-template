<script setup lang="ts">
import type { UploadFile } from 'antdv-next';

import type { VbenFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { ProfileBaseSetting, z } from '@vben/common-ui';

import { message } from 'antdv-next';

import { upload_file } from '#/api/core/upload';
import { getUserInfoApi, updateOwnProfileApi } from '#/api/core/user';
import { useAuthStore } from '#/store';

const authStore = useAuthStore();
const profileBaseSettingRef = ref<InstanceType<typeof ProfileBaseSetting>>();
const saving = ref(false);

const formSchema = computed((): VbenFormSchema[] => [
  {
    fieldName: 'realName',
    component: 'Input',
    label: '姓名',
    rules: z.string().trim().min(1, '请输入姓名').max(128),
  },
  {
    fieldName: 'username',
    component: 'Input',
    componentProps: { disabled: true },
    label: '用户名',
  },
  {
    fieldName: 'email',
    component: 'Input',
    label: '邮箱',
  },
  {
    fieldName: 'phone',
    component: 'Input',
    label: '手机号',
  },
  {
    fieldName: 'avatarFiles',
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
    label: '头像',
    renderComponentContent: () => ({ default: () => '上传图片' }),
  },
]);

onMounted(async () => {
  const data = await getUserInfoApi();
  await profileBaseSettingRef.value?.getFormApi().setValues({
    ...data,
    avatarFiles: data.avatar
      ? [{
          uid: 'profile-avatar',
          name: '头像',
          status: 'done',
          type: 'image/png',
          url: data.avatar,
        }]
      : [],
    email: data.email || '',
    phone: data.phone || '',
  });
});

async function handleSubmit(values: Record<string, unknown>) {
  if (saving.value) return;
  const avatarFile = (values.avatarFiles as undefined | UploadFile[])?.[0];
  if (avatarFile?.status === 'uploading')
    return message.warning('头像正在上传，请稍后保存');
  if (avatarFile?.status === 'error')
    return message.warning('头像上传失败，请重新上传');
  const avatarUrl = avatarFile?.url ?? avatarFile?.response?.url ?? null;
  if (avatarFile && !avatarUrl)
    return message.warning('头像尚未上传完成');
  saving.value = true;
  try {
    await updateOwnProfileApi({
      realName: String(values.realName).trim(),
      email: String(values.email || '').trim() || null,
      phone: String(values.phone || '').trim() || null,
      avatarUrl,
    });
    await authStore.fetchUserInfo();
    message.success('基本资料已更新');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <ProfileBaseSetting
    ref="profileBaseSettingRef"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
