function requireEnv(name: keyof ImportMetaEnv): string {
  const value = import.meta.env[name];

  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }

  return value;
}

function optionalEnv(name: keyof ImportMetaEnv): string | undefined {
  const value = import.meta.env[name];

  if (value === undefined || value === '') {
    return undefined;
  }

  return value;
}

export const env = {
  apiBaseUrl: requireEnv('VITE_API_BASE_URL'),
  icpNumber: optionalEnv('VITE_ICP_NUMBER'),
  icpLink: optionalEnv('VITE_ICP_LINK') ?? 'https://beian.miit.gov.cn/',
  psbNumber: optionalEnv('VITE_PSB_NUMBER'),
  psbLink: optionalEnv('VITE_PSB_LINK') ?? 'https://www.beian.gov.cn/',
  copyrightOwner: optionalEnv('VITE_COPYRIGHT_OWNER') ?? '曾祥瑞',
} as const;
