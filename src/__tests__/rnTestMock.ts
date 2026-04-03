import { vkAuthMock } from './vkAuthMock';

export function createReactNativeMock() {
  return {
    NativeModules: { VkAuth: vkAuthMock },
    Platform: {
      OS: 'ios',
      select: <T>(spec: { ios?: T; default?: T }) =>
        spec.ios !== undefined ? spec.ios : spec.default,
    },
    NativeEventEmitter: jest.fn().mockImplementation(() => ({
      addListener: jest.fn(),
      removeAllListeners: jest.fn(),
    })),
    requireNativeComponent: jest.fn(() => 'View'),
  };
}
