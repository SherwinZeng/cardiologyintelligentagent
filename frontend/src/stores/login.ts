import {defineStore} from "pinia";
import type {ILoginState} from "@/typings/state/loginState.ts";

export const useUserLoginStore = defineStore("userLogin", () => {
    const userLoginStore = reactive<ILoginState>({
        username: "",
        id: "",
        token: "",
        avatar: ""
    })

    const changeUserLoginStore = (loginState: ILoginState) => {
        userLoginStore.username = loginState.username;
        userLoginStore.id = loginState.id;
        userLoginStore.token = loginState.token;
        userLoginStore.avatar = loginState.avatar;
    }

    return {
        userLoginStore,
        changeUserLoginStore,
    }
})