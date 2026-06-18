import type { IBaseResponse } from '@/typings/baseResponse.ts';

export async function fetchCaptcha(
  phone: string,
): Promise<{ captchaId: string; captchaImage: string }> {
  const response = await fetch(
    `${import.meta.env.VITE_AUTH_API_BASE_URL}/auth/sms/login/captcha/v1`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone }),
    },
  );
  const result = (await response.json()) as IBaseResponse<{
    captchaId: string;
    captchaImage: string;
  }>;
  if (!response.ok || result.code !== 200) {
    throw new Error(result.message || '图形验证码获取失败');
  }
  return result.data;
}
