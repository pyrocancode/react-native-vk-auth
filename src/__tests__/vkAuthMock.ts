export const vkAuthMock = {
  initialize: jest.fn(),
  getUserProfile: jest.fn(async () => ({})),
  getUserSessions: jest.fn(async () => []),
  startAuth: jest.fn(),
  closeAuth: jest.fn(),
  logout: jest.fn(),
  openURL: jest.fn(),
  accessTokenChangedSuccess: jest.fn(),
  accessTokenChangedFailed: jest.fn(),
};
