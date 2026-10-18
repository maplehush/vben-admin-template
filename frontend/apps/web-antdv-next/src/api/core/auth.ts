import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    username: string;
    password: string;
    captchaId: string;
    captchaCode: string;
  }

  export interface CaptchaResult {
    captchaId: string;
    imageData: string;
    expiresInSeconds: number;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  // Login errors must not enter the 401 refresh interceptor.
  const response = await baseRequestClient.post<{
    data: { code: number; data: AuthApi.LoginResult; message: string };
  }>('/auth/login', data, { withCredentials: true });
  return response.data.data;
}

export async function getCaptchaApi() {
  const response = await baseRequestClient.get<{
    data: { code: number; data: AuthApi.CaptchaResult; message: string };
  }>('/auth/captcha');
  return response.data.data;
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  return baseRequestClient.post<AuthApi.RefreshTokenResult>(
    '/auth/refresh',
    undefined,
    {
      withCredentials: true,
    },
  );
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return baseRequestClient.post('/auth/logout', undefined, {
    withCredentials: true,
  });
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
