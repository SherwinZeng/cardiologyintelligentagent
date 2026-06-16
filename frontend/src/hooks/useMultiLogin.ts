import type {IUseMultiLoginReturn} from "@/typings/hooks/useMultiLoginReturn.ts";
import type {Router} from "vue-router";
import {v4 as uuidV4} from "uuid"
import {ElMessage} from "element-plus";
import {handleGuestLoginService} from "@/services/login/handleGuestLogin.ts";
import {handleSendSmsCodeService} from "@/services/login/handleSendSmsCode.ts";
import {handleSmsLoginService} from "@/services/login/handleSmsLogin.ts";
import type {IGuestLoginResponse} from "@/typings/login/guestLogin.ts";
import type {ISmsLoginResponse} from "@/typings/login/smsLogin.ts";
import type {IBaseResponse} from "@/typings/baseResponse.ts";
import { useUserLoginStore } from "@/stores/login.ts";
import {useAsyncAction} from "@/hooks/useAsyncAction.ts";
import {getApiErrorMessage} from "@/http";
import { resolveUserDisplayName } from "@/utils/resolveUserDisplayName.ts";
import { resolvePostLoginRedirect } from "@/utils/resolveLoginRedirect.ts";

export const useMultiLogin = (): IUseMultiLoginReturn => {
    const navigateRouter: Router = useRouter()
    const route = useRoute()
    const {persistLogin} = useUserLoginStore()
    const {run} = useAsyncAction()

    function navigateAfterLogin() {
        const redirect = resolvePostLoginRedirect(route.query.redirect)
        void navigateRouter.push(redirect)
    }

    async function sendSmsCode(data: { phone: string; captchaId: string; captchaCode: string }): Promise<boolean> {
        let success = false
        await run<IBaseResponse<null>>(() => handleSendSmsCodeService(data), () => {
            success = true
            ElMessage.success("验证码已发送")
        }, (error) => {
            ElMessage.error(getApiErrorMessage(error))
        })
        return success
    }

    async function handleSmsLogin(data: { phone: string; code: string }) {
        await run<IBaseResponse<ISmsLoginResponse>>(() => handleSmsLoginService(data), (smsLoginResponse) => {
            const profile = smsLoginResponse.data
            persistLogin({
                username: resolveUserDisplayName(profile.nickname, profile.phone),
                phone: profile.phone || data.phone.trim(),
                avatar: profile.avatar || '',
                token: profile.token,
                id: profile.id,
            })
            navigateAfterLogin()
            ElMessage.success("登录成功")
        }, (error) => {
            ElMessage.error(getApiErrorMessage(error))
        })
    }

    async function handleGithubLogin() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleQqLogin() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleGuestLogin(options: { navigate?: boolean } = {}): Promise<boolean> {
        const { navigate = true } = options
        let success = false
        const id: string = uuidV4()
        await run<IBaseResponse<IGuestLoginResponse>>(() => handleGuestLoginService({id}), (guestLoginResponse) => {
            persistLogin({
                username: '访客',
                phone: '',
                avatar: '',
                token: guestLoginResponse.data.token,
                id: guestLoginResponse.data.id,
            })
            if (navigate) {
                navigateAfterLogin()
            }
            ElMessage.success("登录成功,访客有效期仅有1个小时和限30次提问哦")
            success = true
        }, (error) => {
            ElMessage.error(getApiErrorMessage(error))
        })
        return success
    }


    return {
        sendSmsCode,
        handleGithubLogin,
        handleGuestLogin,
        handleQqLogin,
        handleSmsLogin
    }
}