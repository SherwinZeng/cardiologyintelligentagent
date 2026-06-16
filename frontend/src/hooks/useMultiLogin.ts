import type {IUseMultiLoginReturn} from "@/typings/hooks/useMultiLoginReturn.ts";
import type {Router} from "vue-router";
import {v4 as uuidV4} from "uuid"
import {ElMessage} from "element-plus";
import {handleGuestLoginService} from "@/services/login/handleGuestLogin.ts";
import type {IGuestLoginResponse} from "@/typings/login/guestLogin.ts";
import type {IBaseResponse} from "@/typings/baseResponse.ts";
import { useUserLoginStore } from "@/stores/login.ts";
import {useAsyncAction} from "@/hooks/useAsyncAction.ts";
import {getApiErrorMessage} from "@/http";

export const useMultiLogin = (): IUseMultiLoginReturn => {
    const navigateRouter: Router = useRouter()
    const {persistLogin} = useUserLoginStore()
    const {run} = useAsyncAction()

    async function sendSmsCode() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleSmsLogin() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleGithubLogin() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleQqLogin() {
        // 暂未实现
        return Promise.resolve();
    }

    async function handleGuestLogin() {
        const id: string = uuidV4()
        await run<IBaseResponse<IGuestLoginResponse>>(() => handleGuestLoginService({id}), (guestLoginResponse) => {
            persistLogin({
                username: "访客",
                avatar: "",
                token: guestLoginResponse.data.token,
                id: guestLoginResponse.data.id,
            })
            navigateRouter.push("/");
            ElMessage.success("登录成功,访客有效期仅有1个小时和限30次提问哦")
        }, (error) => {
            ElMessage.error(getApiErrorMessage(error))
        })
    }


    return {
        sendSmsCode,
        handleGithubLogin,
        handleGuestLogin,
        handleQqLogin,
        handleSmsLogin
    }
}