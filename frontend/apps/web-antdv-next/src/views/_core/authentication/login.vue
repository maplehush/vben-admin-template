<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, h, onMounted, ref } from 'vue';

import { AuthenticationLogin, z } from '@vben/common-ui';

import { message } from 'antdv-next';

import { getCaptchaApi } from '#/api/core/auth';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const captchaId = ref('');
const captchaImage = ref('');
const captchaLoading = ref(false);
const REMEMBER_ME_KEY = `MAPLE_HUSH_REMEMBER_USERNAME_${location.hostname}`;

// Vben Form renders suffix beside its field, keeping the image and input aligned.
function CaptchaImage() {
  return h(
    'button',
    {
      'aria-label': '刷新验证码',
      class: 'block h-10 w-[124px] overflow-hidden rounded-md border',
      disabled: captchaLoading.value,
      onClick: refreshCaptcha,
      onKeydown: (event: KeyboardEvent) => event.stopPropagation(),
      title: '刷新验证码',
      type: 'button',
    },
    captchaImage.value
      ? h('img', {
          alt: '图片验证码',
          class: 'h-full w-full object-fill',
          src: captchaImage.value,
        })
      : '刷新验证码',
  );
}

const formSchema = computed((): VbenFormSchema[] => [
  {
    component: 'VbenInput',
    componentProps: { placeholder: '请输入账号', autocomplete: 'username' },
    fieldName: 'username',
    label: '账号',
    rules: z.string().min(1, { message: '请输入账号' }),
  },
  {
    component: 'VbenInputPassword',
    componentProps: { placeholder: '请输入密码', autocomplete: 'current-password' },
    fieldName: 'password',
    label: '密码',
    rules: z.string().min(1, { message: '请输入密码' }),
  },
  {
    component: 'VbenInput',
    componentProps: { placeholder: '请输入图片中的验证码', autocomplete: 'off' },
    fieldName: 'captchaCode',
    label: '验证码',
    rules: z.string().min(1, { message: '请输入验证码' }),
    suffix: CaptchaImage,
  },
]);

async function refreshCaptcha() {
  captchaLoading.value = true;
  try {
    const result = await getCaptchaApi();
    captchaId.value = result.captchaId;
    captchaImage.value = result.imageData;
  } catch {
    captchaId.value = '';
    captchaImage.value = '';
    message.error('验证码获取失败，请点击重试');
  } finally {
    captchaLoading.value = false;
  }
}

async function handleSubmit(values: Record<string, any>, rememberAccount: boolean) {
  if (!captchaId.value) {
    message.warning('请先获取验证码');
    return;
  }
  try {
    await authStore.authLogin(
      {
        username: values.username,
        password: values.password,
        captchaCode: values.captchaCode,
        captchaId: captchaId.value,
      },
      () => {
        // Save only the account name, and only after server login succeeds.
        localStorage.setItem(
          REMEMBER_ME_KEY,
          rememberAccount ? values.username : '',
        );
      },
    );
  } catch (error: any) {
    message.error(error?.response?.data?.message || '登录失败，请重试');
    await refreshCaptcha();
  }
}

onMounted(refreshCaptcha);
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :remember-username-storage-key="REMEMBER_ME_KEY"
    :save-remembered-username-on-submit="false"
    :show-code-login="false"
    :show-forget-password="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    sub-title="请输入账号信息以开始管理您的项目"
    title="欢迎回来"
    @submit="handleSubmit"
  />
</template>
