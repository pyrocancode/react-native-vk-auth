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

/// Публичный клиент + PKCE внутри SDK. Для обмена кода на бэкенде позже можно сменить на `confidentialClientFlow`.
fileprivate func vkAuthMakeConfiguration() -> AuthConfiguration {
    AuthConfiguration(
        flow: .publicClientFlow(),
        scope: Scope([])
    )
}

/// React Native мост для VK ID SDK. См. https://id.vk.com/about/business/go/docs/ru/vkid/latest/vk-id/connection/migration/ios/migration-ios
@objc(VkAuth)
final class VkAuth: RCTEventEmitter {
    fileprivate static weak var _sharedEmitter: VkAuth?

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

    @objc func startAuth() {
        guard let presenter = vkAuthTopViewController() else {
            os_log("No presenter for VKID authorize", type: .error)
            return
        }
        VKID.shared.authorize(
            with: vkAuthMakeConfiguration(),
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
        do {
            let session = try result.get()
            send(event: .onAuth(userSession: session))
        } catch AuthError.cancelled {
            os_log("VKID auth cancelled", type: .info)
        } catch {
            os_log("VKID auth failed: %{public}@", type: .error, error.localizedDescription)
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

        let button = OneTapButton(
            layout: .regular(
                height: .medium(.h44),
                cornerRadius: 8
            ),
            presenter: .uiViewController(root),
            authConfiguration: vkAuthMakeConfiguration(),
            onCompleteAuth: nil
        )

        guard let view = try? VKID.shared.ui(for: button).uiView() else {
            os_log("OneTapButton uiView failed", type: .error)
            return UIView()
        }
        return view
    }
}
