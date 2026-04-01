# @pyrocancode/react-native-vk-auth

Тонкая обёртка над **VK ID SDK** для React Native: **OAuth 2.1**, нативные модули iOS и Android. Репозиторий: [pyrocancode/react-native-vk-auth](https://github.com/pyrocancode/react-native-vk-auth).

## Installation

```sh
npm install @pyrocancode/react-native-vk-auth
```

Альтернатива — установка из GitHub (конкретная ветка или коммит):

```sh
npm install github:pyrocancode/react-native-vk-auth
```

## Настройка iOS проекта

Шаг 1. Установка Cocoapods

```sh
cd ios && pod install
```

Шаг 2. Поддержка URL схемы 
Чтобы пользователь мог авторизоваться бесшовно, SDK взаимодействует с клиентом VK на устройстве пользователя. Если в клиенте есть активная сессия, пользователь увидит свои данные (имя, аватарку и телефон) в кнопках и шторке. Авторизация завершится в один клик по кнопке "Продолжить как 'username'".

Чтобы переход за авторизацией в клиент VK работал, необходимо поддержать схему URL. Для этого добавьте схему vkauthorize-silent в ключ LSApplicationQueriesSchemes в Info.plist.

#### Пример записи схемы в Info.plist

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>vkauthorize-silent</string>
</array>
```

#### Universal Link
Для работы бесшовной авторизации необходимо поддержать Universal Link. При создании приложения на сайте платформы, вам нужно было указать Universal Link, по которому клиент VK будет открывать ваше приложение.

Для этого вам необходимо поддержать Universal Links в вашем проекте.

#### Deep Link
Иногда iOS некорректно обрабатывает Universal Links и они перестают работать в приложении. В этом случае нужны Deep Links, чтобы вернуть пользователя из приложения VK, так как они работают всегда. В этом случае в ваше приложение не будет передана информация о пользователе, но он вернется из клиента VK. Вам нужно поддержать Deep Link вида: vkAPP_ID://, где, APP_ID — идентификатор приложения

#### Пример записи DeepLink в Info.plist
```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
      <key>CFBundleTypeRole</key>
      <string>Editor</string>
      <key>CFBundleURLName</key>
      <string>demo_app</string>
      <key>CFBundleURLSchemes</key>
      <array>
          <string>vk123456</string>
      </array>
  </dict>
</array>
```

#### Обработка ссылки авторизации

В классе ApplicationDelegate вам необходимо добавить менеджер обработки ссылок

```objective-c
#import <React/RCTLinkingManager.h>

- (BOOL)application:(UIApplication *)application
   openURL:(NSURL *)url
   options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options
{
  return [RCTLinkingManager application:application openURL:url options:options];
}

- (BOOL)application:(UIApplication *)application continueUserActivity:(nonnull NSUserActivity *)userActivity
 restorationHandler:(nonnull void (^)(NSArray<id<UIUserActivityRestoring>> * _Nullable))restorationHandler
{
 return [RCTLinkingManager application:application
                  continueUserActivity:userActivity
                    restorationHandler:restorationHandler];
}
```

Подключите `Linking` из React Native и `VK` из этого пакета — в обработчике вызывайте `VK.openURL`, чтобы завершить OAuth-поток после возврата из клиента VK.

```tsx
import { Linking } from 'react-native';
import { VK } from '@pyrocancode/react-native-vk-auth';

React.useEffect(() => {
  Linking.getInitialURL()
    .then((url) => {
      if (url) handleOpenURL({ url });
    })
    .catch((err) => console.warn('getInitialURL', err));

  const sub = Linking.addEventListener('url', handleOpenURL);
  return () => sub.remove();
}, []);

function handleOpenURL(event: { url: string }) {
  VK.openURL(event.url);
}
```

## Minimal setup guide for Android part:
1. Добавить ваши credentials от артифактори в build.gradle проекта

```gradle
// project build.gradle
buildscript { }
allProjects {
  repositories {
    maven {
       url("https://artifactory-external.vkpartner.ru/artifactory/superappkit-maven-public/")
    }
  }
}
```

2. Добавить VkExternalAuthRedirectScheme и VkExternalAuthRedirectHost в build.gradle application’a:
```gradle
// app build.gradle
android { }
android.defaultConfig.manifestPlaceholders = [
    'VkExternalAuthRedirectScheme' : 'vk<ClientId>',
    'VkExternalAuthRedirectHost' : 'vk.com',
]

dependencies { }
```

3. Добавить client_id, client_secret, vk_external_oauth_redirect_url и vk_account_manager_id в strings.xml:
```xml
<integer name="com_vk_sdk_AppId">your_client_id</integer>

<string name="vk_client_secret" translatable="false">your_client_secret</string>

<!-- Template: vk<ClientId>://vk.com -->
<string name="vk_external_oauth_redirect_url" translatable="false">vk<ClientId>://vk.com</string>

<!-- Template: your.package.account -->
<string name="vk_account_manager_id" translatable="false">your.package.account</string>
```

## Минимальный общий сценарий (JS)

Инициализация выполняется **один раз** при старте приложения. Используйте `VK`, `VKID` и при необходимости `VKOneTapButton` из `@pyrocancode/react-native-vk-auth`.

### 1. Инициализация

```tsx
import { Image } from 'react-native';
import { VK, VKID } from '@pyrocancode/react-native-vk-auth';

const logo = Image.resolveAssetSource(require('./assets/logo.png'));

const vkid = new VKID(
  'Моё приложение',
  '1.0.0',
  logo,
  {
    serviceUserAgreement: 'https://example.com/terms',
    servicePrivacyPolicy: 'https://example.com/privacy',
    serviceSupport: null,
  }
);

VK.initialize(
  {
    credentials: {
      clientId: 'YOUR_CLIENT_ID',
      clientSecret: 'YOUR_CLIENT_SECRET',
    },
    mode: VK.Mode.DEBUG,
  },
  vkid
);
```

#### Режим `authFlow`

По умолчанию после входа на устройстве доступен **access token** (`payload.accessToken`, `payload.userId`).

Если нужно обменять код на своём бэкенде (PKCE + `code` / `device_id` / `state` / `code_verifier`), передайте при инициализации:

```tsx
VK.initialize(
  {
    credentials: {
      clientId: 'YOUR_CLIENT_ID',
      clientSecret: 'YOUR_CLIENT_SECRET',
    },
    mode: VK.Mode.DEBUG,
    authFlow: 'authorizationCode',
  },
  vkid
);
```

В `onAuthorized` тогда придёт `payload.authorizationCode` с полями `code`, `codeVerifier`, `state`, `deviceId`, `isCompletion` (без `accessToken` в этом потоке). Поддерживается на **iOS и Android**.

### 2. Подписка на авторизацию и выход

```tsx
vkid.setOnAuthChanged({
  onAuthorized(payload) {
    // OAuth 2.1: access token и идентификатор пользователя VK
    console.log(payload.accessToken, payload.userId, payload.profile);
  },
  onLogout() {
    console.log('Пользователь вышел');
  },
  onAuthFailed(message) {
    console.warn(message);
  },
});
```

### 3. Кнопка One Tap или ручной старт

```tsx
import { Button } from 'react-native';
import { VKOneTapButton, VKID } from '@pyrocancode/react-native-vk-auth';

// В разметке
<VKOneTapButton />

// Или кнопка «Войти через VK»
<Button title="Войти через VK" onPress={() => vkid.startAuth()} />

// Принудительно закрыть экран авторизации
function forceCloseAuth() {
  vkid.closeAuth();
}

function logout() {
  vkid.logout();
}

async function checkLoggedIn() {
  const sessions = await vkid.userSessions();
  return sessions.some((s) => s instanceof VKID.Session.Authorized);
}
```

### Кастомизация One Tap

```tsx
import { VKOneTapButton, VKOneTapButtonSpace } from '@pyrocancode/react-native-vk-auth';

<VKOneTapButton
  style={styles.vkView}
  backgroundStyle={{
    style: VKOneTapButtonSpace.BgColor.CUSTOM,
    customVkIconColor: '#fff',
    customBackgroundColor: '#0077FF',
    customTextColor: '#fff',
  }}
  iconGravity={VKOneTapButtonSpace.IconGravity.START}
  firstLineFieldType={VKOneTapButtonSpace.LineFieldType.ACTION}
  secondLineFieldType={VKOneTapButtonSpace.LineFieldType.PHONE}
  texts={{
    noUserText: 'Войти через VK',
    actionText: 'Продолжить как {firstName} {lastName}',
    phoneText: 'Телефон {phone}',
  }}
  oneLineTextSize={16}
  firstLineTextSize={16}
  secondLineTextSize={14}
  avatarSize={64}
  iconSize={64}
  progressSize={56}
/>
```

## Usage

После настройки iOS/Android (см. выше) используйте API из раздела «Минимальный общий сценарий»: `VK.initialize`, `vkid.setOnAuthChanged`, `vkid.startAuth` / `VKOneTapButton`, `VK.openURL` для deep link.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

[MIT](LICENSE). См. полный текст в файле `LICENSE`.
