export function buildDefaultUserName(phone?: string | null): string {
  const trimmedPhone = phone?.trim();
  if (!trimmedPhone) {
    return '用户';
  }
  if (trimmedPhone.length <= 4) {
    return `用户${trimmedPhone}`;
  }
  return `用户${trimmedPhone.slice(-4)}`;
}

/** 有昵称用昵称，否则用「用户 + 手机后四位」（如图2） */
export function resolveUserDisplayName(nickname?: string | null, phone?: string | null): string {
  const trimmedNickname = nickname?.trim();
  if (trimmedNickname) {
    return trimmedNickname;
  }
  return buildDefaultUserName(phone);
}
