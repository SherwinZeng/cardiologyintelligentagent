export interface IUseMultiLoginReturn {
  sendSmsCode: (data: {
    phone: string;
    captchaId: string;
    captchaCode: string;
  }) => Promise<boolean>;
  handleSmsLogin: (data: { phone: string; code: string }) => Promise<void>;
  handleGuestLogin: (options?: { navigate?: boolean }) => Promise<boolean>;
  handleGithubLogin: () => void;
  handleQqLogin: () => void;
}
