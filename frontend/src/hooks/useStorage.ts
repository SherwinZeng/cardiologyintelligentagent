import type {IUseLocalStorageReturn} from "@/typings/hooks/useStorageReturn.ts";

export const useLocalStorage = (): IUseLocalStorageReturn => {
    const getItem = (key: string): string => {
        return localStorage.getItem(key) ? localStorage.getItem(key) as string : ""
    }
    const setItem = (key: string, value: string): void => {
        localStorage.setItem(key, value);
    }
    const removeItem = (key: string): void => {
        localStorage.removeItem(key);
    }
    return {
        getItem,
        setItem,
        removeItem,
    }
}

export const useSessionStorage = () => {

}
