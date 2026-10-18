import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}

export async function changeOwnPasswordApi(data: {
  oldPassword: string;
  newPassword: string;
}) {
  return requestClient.put<boolean>('/user/password', data);
}

export async function updateOwnProfileApi(data: {
  realName: string;
  email: null | string;
  phone: null | string;
  avatarUrl: null | string;
}) {
  return requestClient.put<boolean>('/user/profile', data);
}
