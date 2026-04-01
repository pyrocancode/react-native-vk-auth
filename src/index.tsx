import React from 'react';
import {
  NativeModules,
  Platform,
  ImageResolvedAssetSource,
  NativeEventEmitter,
  requireNativeComponent,
  ViewProps,
  NativeModule,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-vk-auth' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

interface VkAuthModule extends NativeModule {
  getUserProfile: () => Promise<VKID.UserProfile>;
  startAuth: () => void;
  closeAuth: () => void;
  logout: () => void;
  getUserSessions: () => Promise<Array<{ type: string }>>;
  initialize: (app: VK.App, vkid: VKIDInitPayload) => void;
  openURL: (url: string) => void;

  accessTokenChangedSuccess: (token: string, userId: number) => void;
  accessTokenChangedFailed: (error: Record<string, unknown>) => void;
}

const VkAuth: VkAuthModule = NativeModules.VkAuth
  ? NativeModules.VkAuth
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/** Плоский объект для нативного моста (см. jsinput/VKID.kt). */
export type VKIDInitPayload = {
  appName: string;
  appVersion: string;
  appIcon: ImageResolvedAssetSource;
  appLinks: VKID.Links;
};

export class VK {
  static initialize(app: VK.App, vkid: VKID) {
    VkAuth.initialize(app, {
      appName: vkid.appName,
      appVersion: vkid.appVersion,
      appIcon: vkid.appIcon,
      appLinks: vkid.appLinks,
    });
  }

  static openURL(url: string) {
    VkAuth.openURL(url);
  }
}

export namespace VK {
  /**
   * `authorizationCode` — PKCE на клиенте, в JS придёт `code` + `codeVerifier` + `state` + `deviceId`
   * (Android: VKIDAuthParams.codeChallenge; iOS: confidential client flow + AuthCodeHandler).
   * По умолчанию — готовый access token на устройстве.
   */
  export type AuthFlowMode = 'accessToken' | 'authorizationCode';

  export interface App {
    mode: Mode;
    credentials: Credentials;
    authFlow?: AuthFlowMode;
    /** Запрашиваемые доступы, например `phone`, `email` (включите их в кабинете VK ID для приложения). */
    scopes?: string[];
  }

  export enum Mode {
    DEBUG = 'DEBUG',
    TEST = 'TEST',
    RELEASE = 'RELEASE',
  }

  export interface Credentials {
    clientId: string;
    clientSecret: string;
  }
}

function parseOnAuthPayload(
  raw: Record<string, unknown>
): { kind: 'error'; message: string } | { kind: 'ok'; payload: VKID.AuthSuccessPayload } {
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
          profile: raw.profile as VKID.UserProfile | undefined,
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
          profile: raw.profile as VKID.UserProfile | undefined,
        },
      };
    }
  }

  return { kind: 'error', message: 'Unexpected VK auth payload' };
}

function profileUserIdFromProfile(profile: unknown): string {
  if (!profile || typeof profile !== 'object') {
    return '';
  }
  const u = (profile as { userID?: { value?: string } }).userID;
  return u?.value != null ? String(u.value) : '';
}

export class VKID {
  readonly appName: string;
  readonly appVersion: string;
  readonly appIcon: ImageResolvedAssetSource;
  readonly appLinks: VKID.Links;
  private readonly eventEmitter: NativeEventEmitter;

  constructor(
    appName: string,
    appVersion: string,
    appIcon: ImageResolvedAssetSource,
    appLinks: VKID.Links
  ) {
    this.appName = appName;
    this.appVersion = appVersion;
    this.appIcon = appIcon;
    this.appLinks = appLinks;
    this.eventEmitter = new NativeEventEmitter(VkAuth);
  }

  startAuth() {
    VkAuth.startAuth();
  }

  closeAuth() {
    VkAuth.closeAuth();
  }

  async getUserProfile(): Promise<VKID.UserProfile> {
    return await VkAuth.getUserProfile();
  }

  logout() {
    VkAuth.logout();
  }

  userSessions(): Promise<Array<VKID.Session.UserSession>> {
    return VkAuth.getUserSessions().then(sessions => {
      return sessions.map(session => {
        return session.type === UserSessionInternal.Type.AUTHORIZED
          ? new VKID.Session.Authorized()
          : new VKID.Session.Authenticated();
      });
    });
  }

  setOnAuthChanged(callbacks: VKID.AuthChangedCallback) {
    this.eventEmitter.removeAllListeners('onLogout');
    this.eventEmitter.removeAllListeners('onAuth');

    this.eventEmitter.addListener('onLogout', () => {
      callbacks.onLogout();
    });

    this.eventEmitter.addListener(
      'onAuth',
      (raw: Record<string, unknown>) => {
        const parsed = parseOnAuthPayload(raw);
        if (parsed.kind === 'error') {
          callbacks.onAuthFailed?.(parsed.message);
          return;
        }
        callbacks.onAuthorized(parsed.payload);
      }
    );
  }
}

export namespace VKID {
  export class Token {
    value: string;

    constructor(value: string) {
      this.value = value;
    }
  }

  export class UserID {
    value: bigint;

    constructor(value: bigint) {
      this.value = value;
    }
  }

  export interface Links {
    serviceUserAgreement: string;
    servicePrivacyPolicy: string;
    serviceSupport: string | null;
  }

  export interface UserProfile {
    userID: UserID;
    firstName: string | null;
    lastName: string | null;
    phone: string | null;
    photo200: string | null;
    email: string | null;
    userHash: string | null;
  }

  /** Успешная авторизация VK ID (OAuth 2.1): access token и id пользователя VK. */
  export interface AuthSuccessPayload {
    accessToken?: string;
    userId?: string;
    profile?: UserProfile;
    /** Android: полный ответ VK ID SDK (отладка). */
    vkidNative?: Record<string, unknown>;
    /** Поток обмена кода на бэкенде (`authFlow: 'authorizationCode'`). */
    authorizationCode?: {
      code: string;
      codeVerifier: string;
      state: string;
      deviceId: string;
      isCompletion: boolean;
    };
  }

  export interface AuthChangedCallback {
    onAuthorized(payload: AuthSuccessPayload): void;
    onLogout(): void;
    onAuthFailed?(message: string): void;
  }

  export namespace Session {
    export abstract class UserSession {}

    export class Authorized extends UserSession {
      constructor() {
        super();
      }

      get userProfile(): Promise<VKID.UserProfile> {
        return VkAuth.getUserProfile();
      }

      toString(): string {
        return 'Authorized';
      }
    }

    export class Authenticated extends UserSession {
      toString(): string {
        return 'Authenticated';
      }
    }
  }
}

namespace UserSessionInternal {
  export class UserSession {
    private readonly _type: UserSessionInternal.Type;

    constructor(type: Type) {
      this._type = type;
    }

    get type(): Type {
      return this._type;
    }
  }

  export enum Type {
    AUTHORIZED = 'authorized',
    AUTHENTICATED = 'authenticated',
  }
}

export namespace VKOneTapButtonSpace {
  export const nativeView = requireNativeComponent('RTCVkOneTapButton');

  export enum BgColor {
    BLUE = 'BLUE',
    WHITE = 'WHITE',
    CUSTOM = 'CUSTOM',
  }

  export interface BgStyle {
    style: BgColor;
    customVkIconColor?: string;
    customBackgroundColor?: string;
    customTextColor?: string;
  }

  export enum IconGravity {
    START = 'START',
    TEXT = 'TEXT',
  }

  export enum LineFieldType {
    ACTION = 'ACTION',
    PHONE = 'PHONE',
    NONE = 'NONE',
  }

  export interface Texts {
    noUserText?: string;
    actionText?: string;
    phoneText?: string;
  }

  export type Props = {
    backgroundStyle?: BgStyle | undefined;
    iconGravity?: IconGravity | undefined;
    firstLineFieldType?: LineFieldType | undefined;
    secondLineFieldType?: LineFieldType | undefined;
    oneLineTextSize?: number | undefined;
    firstLineTextSize?: number | undefined;
    secondLineTextSize?: number | undefined;
    avatarSize?: number | undefined;
    iconSize?: number | undefined;
    progressSize?: number | undefined;
    texts?: Texts | undefined;
  } & ViewProps;
}

export class VKOneTapButton extends React.Component<VKOneTapButtonSpace.Props> {
  render() {
    // @ts-ignore
    return <VKOneTapButtonSpace.nativeView {...this.props} />;
  }
}
