/**
 * Результат разбора события `onAuth` с нативного моста.
 * Структура совместима с `VKID.AuthSuccessPayload` в `index.tsx`.
 */
export type AuthSuccessPayloadParsed = {
  accessToken?: string;
  userId?: string;
  profile?: unknown;
  vkidNative?: Record<string, unknown>;
  authorizationCode?: {
    code: string;
    codeVerifier: string;
    state: string;
    deviceId: string;
    isCompletion: boolean;
  };
};

export type ParsedOnAuthResult =
  | { kind: 'error'; message: string }
  | { kind: 'ok'; payload: AuthSuccessPayloadParsed };

function profileUserIdFromProfile(profile: unknown): string {
  if (!profile || typeof profile !== 'object') {
    return '';
  }
  const u = (profile as { userID?: { value?: string } }).userID;
  return u?.value != null ? String(u.value) : '';
}

export function parseOnAuthPayload(raw: Record<string, unknown>): ParsedOnAuthResult {
  if (raw.type === 'error') {
    return {
      kind: 'error',
      message: String(raw.error ?? 'VK ID authorization failed'),
    };
  }

  if (raw.type === 'authorization_code') {
    if (
      typeof raw.code === 'string' &&
      typeof raw.codeVerifier === 'string' &&
      typeof raw.state === 'string'
    ) {
      return {
        kind: 'ok',
        payload: {
          authorizationCode: {
            code: raw.code,
            codeVerifier: raw.codeVerifier,
            state: raw.state,
            deviceId: typeof raw.deviceId === 'string' ? raw.deviceId : '',
            isCompletion: Boolean(raw.isCompletion),
          },
        },
      };
    }
    return { kind: 'error', message: 'Invalid authorization_code payload' };
  }

  if (raw.type === 'authorized') {
    const vkid = raw.vkid as Record<string, unknown> | undefined;
    if (vkid && typeof vkid.accessToken === 'string') {
      const uid =
        vkid.userID != null && String(vkid.userID) !== ''
          ? String(vkid.userID)
          : profileUserIdFromProfile(raw.profile);
      if (!uid) {
        return { kind: 'error', message: 'VK user id missing in auth payload' };
      }
      return {
        kind: 'ok',
        payload: {
          accessToken: vkid.accessToken,
          userId: uid,
          profile: raw.profile,
          vkidNative: vkid,
        },
      };
    }

    if (typeof raw.accessToken === 'string' && typeof raw.userId === 'string') {
      if (!raw.userId) {
        return { kind: 'error', message: 'VK user id missing in auth payload' };
      }
      return {
        kind: 'ok',
        payload: {
          accessToken: raw.accessToken,
          userId: raw.userId,
          profile: raw.profile,
        },
      };
    }
  }

  return { kind: 'error', message: 'Unexpected VK auth payload' };
}
