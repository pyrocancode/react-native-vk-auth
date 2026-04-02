import UIKit
import OSLog
import React
import VKID
import VKIDCore

// MARK: - Общие хелперы (VK ID SDK 2.x, OAuth 2.1)

fileprivate func vkAuthTopViewController() -> UIViewController? {
    let keyWindow: UIWindow?
    if #available(iOS 13.0, *) {
        keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
    } else {
        keyWindow = UIApplication.shared.keyWindow
    }
    guard var top = keyWindow?.rootViewController else { return nil }
    while let presented = top.presentedViewController {
        top = presented
    }
    return top
}

/// Обмен кода на бэкенде: параметры уходят в JS; `finishFlow` завершает UI SDK (см. `AuthCodeHandler`).
private final class RnAuthorizationCodeHandler: NSObject, AuthCodeHandler {
    weak var emitter: VkAuth?

    func exchange(_ code: AuthorizationCode, finishFlow: @escaping () -> Void) {
        emitter?.sendAuthorizationCodePayload(code)
        finishFlow()
    }
}

/// React Native мост для VK ID SDK. См. https://id.vk.com/about/business/go/docs/ru/vkid/latest/vk-id/connection/migration/ios/migration-ios
@objc(VkAuth)
final class VkAuth: RCTEventEmitter {
    fileprivate static weak var _sharedEmitter: VkAuth?

    private let authorizationCodeHandler = RnAuthorizationCodeHandler()
    private var useAuthorizationCodeFlow = false
    private var requestedScopes: [String] = []

    @objc(initialize:vkid:)
    func initialize(_ app: NSDictionary, vkid: NSDictionary) {
        guard
            let credentials = app["credentials"] as? [String: Any],
            let clientId = credentials["clientId"] as? String,
            let clientSecret = credentials["clientSecret"] as? String
        else {
            os_log("Incorrect credentials format", type: .error)
            return
        }

        if let flow = app["authFlow"] as? String, flow == "authorizationCode" {
            useAuthorizationCodeFlow = true
        } else {
            useAuthorizationCodeFlow = false
        }

        if let scopes = app["scopes"] as? [String] {
            requestedScopes = scopes
        } else if let scopes = app["scopes"] as? [Any] {
            requestedScopes = scopes.compactMap { $0 as? String }
        } else {
            requestedScopes = []
        }

        do {
            try VKID.shared.set(
                config: Configuration(
                    appCredentials: AppCredentials(
                        clientId: clientId,
                        clientSecret: clientSecret
                    )
                )
            )
            VkAuth._sharedEmitter = self
            authorizationCodeHandler.emitter = self
            VKID.shared.add(observer: self)
        } catch {
            os_log("VKID initialization failed: %{public}@", type: .error, error.localizedDescription)
        }
    }

    @objc(openURL:)
    func openURL(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        _ = VKID.shared.open(url: url)
    }

    fileprivate func makeScope() -> Scope {
        // VKID.Scope: есть init(_ Set<String>), нет init([String]) — иначе Xcode: «No exact matches in call to initializer»
        Scope(Set(requestedScopes))
    }

    fileprivate func makeAuthConfiguration() -> AuthConfiguration {
        if useAuthorizationCodeFlow {
            return AuthConfiguration(
                flow: .confidentialClientFlow(
                    codeExchanger: authorizationCodeHandler,
                    pkce: nil
                ),
                scope: makeScope()
            )
        }
        return AuthConfiguration(
            flow: .publicClientFlow(),
            scope: makeScope()
        )
    }

    fileprivate func sendAuthorizationCodePayload(_ code: AuthorizationCode) {
        let body: [String: Any] = [
            "type": "authorization_code",
            "code": code.code,
            "deviceId": code.deviceId,
            "state": code.state,
            "codeVerifier": code.codeVerifier ?? "",
            "isCompletion": true,
        ]
        sendEvent(withName: "onAuth", body: body)
    }

    @objc func startAuth() {
        guard let presenter = vkAuthTopViewController() else {
            os_log("No presenter for VKID authorize", type: .error)
            return
        }
        VKID.shared.authorize(
            with: makeAuthConfiguration(),
            using: .uiViewController(presenter)
        ) { _ in }
    }

    @objc func closeAuth() {
        vkAuthTopViewController()?.dismiss(animated: true)
        os_log("Authorization UI dismissed", type: .info)
    }

    @objc func logout() {
        guard let session = VKID.shared.authorizedSessions.first else { return }
        session.logout { [weak self] result in
            if case .success = result {
                self?.send(event: .onLogout)
            }
        }
    }

    @objc(getUserSessions:rejecter:)
    func getUserSessions(
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        resolve(VKID.shared.authorizedSessions.map(\.dictionary))
    }

    @objc(getUserProfile:rejecter:)
    func getUserProfile(
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let session = VKID.shared.authorizedSessions.first else {
            reject("no_session", "No authorized VK ID session", nil)
            return
        }
        session.fetchUser { result in
            switch result {
            case .success:
                if let user = session.user {
                    resolve(user.dictionary)
                } else {
                    resolve(["userID": ["value": String(session.userId.value)]])
                }
            case .failure(let error):
                reject("profile_failed", error.localizedDescription, error)
            }
        }
    }

    @objc(accessTokenChangedSuccess:userId:)
    func accessTokenChangedSuccess(_ token: String, userId: NSNumber) {
        os_log("accessTokenChangedSuccess ignored on VK ID iOS (no silent exchange)", type: .info)
    }

    @objc(accessTokenChangedFailed:)
    func accessTokenChangedFailed(_ error: NSDictionary) {
        os_log("accessTokenChangedFailed ignored on VK ID iOS", type: .info)
    }

    @objc(supportedEvents)
    override func supportedEvents() -> [String]! {
        ["onLogout", "onAuth"]
    }
}

extension VkAuth: VKIDObserver {
    func vkid(_ vkid: VKID, didCompleteAuthWith result: AuthResult, in oAuth: OAuthProvider) {
        switch result {
        case .success(let session):
            send(event: .onAuth(userSession: session))
        case .failure(AuthError.authCodeExchangedOnYourBackend):
            // Код уже отправлен в JS из RnAuthorizationCodeHandler.exchange
            break
        case .failure(AuthError.cancelled):
            os_log("VKID auth cancelled", type: .info)
        case .failure(let error):
            os_log("VKID auth failed: %{public}@", type: .error, String(describing: error))
        }
    }

    func vkid(_ vkid: VKID, didLogoutFrom session: UserSession, with result: LogoutResult) {
        if case .success = result {
            send(event: .onLogout)
        }
    }

    func vkid(_ vkid: VKID, didStartAuthUsing oAuth: OAuthProvider) {}

    func vkid(_ vkid: VKID, didUpdateUserIn session: UserSession, with result: UserFetchingResult) {}

    func vkid(_ vkid: VKID, didRefreshAccessTokenIn session: UserSession, with result: TokenRefreshingResult) {}
}

@objc(RTCVkOneTapButton)
final class OneTapButtonManager: RCTViewManager {
    override func view() -> UIView! {
        guard VkAuth._sharedEmitter != nil else {
            os_log("VkAuth not initialized before OneTap", type: .fault)
            return UIView()
        }
        guard let root = vkAuthTopViewController() else {
            return UIView()
        }

        guard let emitter = VkAuth._sharedEmitter else {
            return UIView()
        }

        let button = OneTapButton(
            layout: .regular(
                height: .medium(.h44),
                cornerRadius: 8
            ),
            presenter: .uiViewController(root),
            authConfiguration: emitter.makeAuthConfiguration(),
            onCompleteAuth: nil
        )

        guard let view = try? VKID.shared.ui(for: button).uiView() else {
            os_log("OneTapButton uiView failed", type: .error)
            return UIView()
        }
        return view
    }
}
