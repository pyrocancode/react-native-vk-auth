jest.mock('react-native', () => require('./rnTestMock').createReactNativeMock());

import type { ImageResolvedAssetSource } from 'react-native';
import { VK } from '../index';
import { vkAuthMock } from './vkAuthMock';

describe('VK.initialize', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('передаёт в нативный мост credentials, authFlow и scopes', () => {
    VK.initialize(
      {
        mode: VK.Mode.RELEASE,
        credentials: { clientId: 'id', clientSecret: 'sec' },
        authFlow: 'authorizationCode',
        scopes: ['phone', 'email'],
      },
      {
        appName: 'App',
        appVersion: '1',
        appIcon: { uri: 'x', width: 1, height: 1, scale: 1 } as ImageResolvedAssetSource,
        appLinks: {
          serviceUserAgreement: 'https://a',
          servicePrivacyPolicy: 'https://p',
          serviceSupport: null,
        },
      }
    );

    expect(vkAuthMock.initialize).toHaveBeenCalledTimes(1);
    expect(vkAuthMock.initialize).toHaveBeenCalledWith(
      {
        mode: 'RELEASE',
        credentials: { clientId: 'id', clientSecret: 'sec' },
        authFlow: 'authorizationCode',
        scopes: ['phone', 'email'],
      },
      {
        appName: 'App',
        appVersion: '1',
        appIcon: { uri: 'x', width: 1, height: 1, scale: 1 },
        appLinks: {
          serviceUserAgreement: 'https://a',
          servicePrivacyPolicy: 'https://p',
          serviceSupport: null,
        },
      }
    );
  });

  it('openURL проксируется в нативный модуль', () => {
    VK.openURL('vk123://cb');
    expect(vkAuthMock.openURL).toHaveBeenCalledWith('vk123://cb');
  });
});
