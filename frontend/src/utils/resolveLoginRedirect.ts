const REDIRECT_QUERY_KEY = 'redirect';

/** 仅允许站内相对路径，避免开放重定向 */
export function sanitizeLoginRedirect(value: unknown, fallback = '/'): string {
  if (typeof value !== 'string') {
    return fallback;
  }

  const trimmed = value.trim();
  if (!trimmed.startsWith('/') || trimmed.startsWith('//')) {
    return fallback;
  }

  return trimmed;
}

export function buildLoginRoute(redirect?: string) {
  const safeRedirect = sanitizeLoginRedirect(redirect);
  if (safeRedirect === '/') {
    return { name: 'login' as const };
  }

  return {
    name: 'login' as const,
    query: { [REDIRECT_QUERY_KEY]: safeRedirect },
  };
}

export function resolvePostLoginRedirect(value: unknown): string {
  return sanitizeLoginRedirect(value, '/');
}
