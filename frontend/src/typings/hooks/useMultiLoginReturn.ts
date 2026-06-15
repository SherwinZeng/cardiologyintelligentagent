export interface IUseMultiLoginReturn {
    sendSmsCode: () => void;
    handleSmsLogin: () => void;
    handleGuestLogin: () => void;
    handleGithubLogin: () => void;
    handleQqLogin: () => void;
}