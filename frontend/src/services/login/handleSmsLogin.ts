import http from "@/http";

export async function handleSmsLoginService<T>(data: any): Promise<T> {
    return http.post<T, T>("/auth/sms/login/v1", data).then((response: T) => {
        return response;
    }, (error) => {
        return Promise.reject(error);
    })
}
