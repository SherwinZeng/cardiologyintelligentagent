import type {IUseMultiLoginReturn} from "@/typings/hooks/useMultiLoginReturn.ts";
import type {Router} from "vue-router";
import {v4 as uuidV4} from "uuid"
import {ElMessage} from "element-plus";
import {handleGuestLoginService} from "@/services/login/handleGuestLogin.ts";
import {useLocalStorage} from "@/hooks/useStorage.ts";
import type {IGuestLoginResponse} from "@/typings/login/guestLogin.ts";
import type {IBaseResponse} from "@/typings/baseResponse.ts";
import {useUserLoginStore} from "@/stores/login.ts";
import {useAsyncAction} from "@/hooks/useAsyncAction.ts";

export const useMultiLogin = (): IUseMultiLoginReturn => {
    const navigateRouter: Router = useRouter()
    const {changeUserLoginStore} = useUserLoginStore()
    const {setItem} = useLocalStorage()
    const {run} = useAsyncAction()

    async function sendSmsCode() {
    }

    async function handleSmsLogin() {
    }

    async function handleGithubLogin() {
    }

    async function handleQqLogin() {
    }

    async function handleGuestLogin() {
        const id: string = uuidV4()
        await run<IBaseResponse<IGuestLoginResponse>>(() => handleGuestLoginService({id}), (guestLoginResponse) => {
            setItem("user_login", JSON.stringify(guestLoginResponse.data))
            changeUserLoginStore({
                username: "访客",
                avatar: "",
                token: guestLoginResponse.data.token,
                id: guestLoginResponse.data.id,
            })
            navigateRouter.push("/");
            ElMessage.success("登录成功,访客有效期仅有1个小时和限30次提问哦")
        }, (error) => {
            ElMessage.error("登录失败，请联系管理员解决")
            console.log(error)
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