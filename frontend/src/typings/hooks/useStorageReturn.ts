export interface IUseLocalStorageReturn {
  getItem: (key: string) => string;
  setItem: (key: string, value: string) => void;
  removeItem: (key: string) => void;
}

export interface ISessionStorageReturn {
  getItem: <T>(key: string) => T | null;
  setItem: <T>(key: string, value: T) => void;
  removeItem: (key: string) => void;
  clear: () => void;
}
