/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_ICP_NUMBER?: string;
  readonly VITE_ICP_LINK?: string;
  readonly VITE_PSB_NUMBER?: string;
  readonly VITE_PSB_LINK?: string;
  readonly VITE_COPYRIGHT_OWNER?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
