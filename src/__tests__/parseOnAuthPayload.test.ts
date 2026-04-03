import { parseOnAuthPayload } from '../parseAuthPayload';

describe('parseOnAuthPayload', () => {
  it('returns error for type error', () => {
    const r = parseOnAuthPayload({ type: 'error', error: 'boom' });
    expect(r).toEqual({ kind: 'error', message: 'boom' });
  });

  it('uses default message for error without error field', () => {
    const r = parseOnAuthPayload({ type: 'error' });
    expect(r).toEqual({ kind: 'error', message: 'VK ID authorization failed' });
  });

  it('parses authorization_code when all string fields present', () => {
    const r = parseOnAuthPayload({
      type: 'authorization_code',
      code: 'c',
      codeVerifier: 'v',
      state: 's',
      deviceId: 'd',
      isCompletion: true,
    });
    expect(r.kind).toBe('ok');
    if (r.kind === 'ok') {
      expect(r.payload.authorizationCode).toEqual({
        code: 'c',
        codeVerifier: 'v',
        state: 's',
        deviceId: 'd',
        isCompletion: true,
      });
    }
  });

  it('authorization_code: empty deviceId becomes empty string', () => {
    const r = parseOnAuthPayload({
      type: 'authorization_code',
      code: 'c',
      codeVerifier: 'v',
      state: 's',
    });
    expect(r.kind).toBe('ok');
    if (r.kind === 'ok') {
      expect(r.payload.authorizationCode?.deviceId).toBe('');
    }
  });

  it('authorization_code: invalid when code missing', () => {
    const r = parseOnAuthPayload({
      type: 'authorization_code',
      codeVerifier: 'v',
      state: 's',
    });
    expect(r).toEqual({ kind: 'error', message: 'Invalid authorization_code payload' });
  });

  it('authorized: nested vkid with accessToken and userID', () => {
    const r = parseOnAuthPayload({
      type: 'authorized',
      vkid: { accessToken: 'tok', userID: '12345' },
    });
    expect(r.kind).toBe('ok');
    if (r.kind === 'ok') {
      expect(r.payload.accessToken).toBe('tok');
      expect(r.payload.userId).toBe('12345');
      expect(r.payload.vkidNative).toEqual({ accessToken: 'tok', userID: '12345' });
    }
  });

  it('authorized: user id from profile.userID.value when vkid.userID empty', () => {
    const r = parseOnAuthPayload({
      type: 'authorized',
      vkid: { accessToken: 'tok' },
      profile: { userID: { value: '99' } },
    });
    expect(r.kind).toBe('ok');
    if (r.kind === 'ok') {
      expect(r.payload.userId).toBe('99');
    }
  });

  it('authorized: fails when accessToken present but no user id', () => {
    const r = parseOnAuthPayload({
      type: 'authorized',
      vkid: { accessToken: 'tok' },
    });
    expect(r).toEqual({ kind: 'error', message: 'VK user id missing in auth payload' });
  });

  it('authorized: flat accessToken + userId', () => {
    const r = parseOnAuthPayload({
      type: 'authorized',
      accessToken: 't2',
      userId: 'u1',
    });
    expect(r.kind).toBe('ok');
    if (r.kind === 'ok') {
      expect(r.payload.accessToken).toBe('t2');
      expect(r.payload.userId).toBe('u1');
    }
  });

  it('authorized: flat userId empty string is error', () => {
    const r = parseOnAuthPayload({
      type: 'authorized',
      accessToken: 't',
      userId: '',
    });
    expect(r).toEqual({ kind: 'error', message: 'VK user id missing in auth payload' });
  });

  it('unexpected payload', () => {
    expect(parseOnAuthPayload({ type: 'unknown' })).toEqual({
      kind: 'error',
      message: 'Unexpected VK auth payload',
    });
  });
});
