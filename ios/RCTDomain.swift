import Foundation
import VKID

protocol RCTDomain {
    var dictionary: [String: Any?] { get }
}

extension User: RCTDomain {
    var dictionary: [String: Any?] {
        [
            "userID": ["value": String(self.id.value)],
            "firstName": self.firstName,
            "lastName": self.lastName,
            "phone": self.phone,
            "photo200": self.avatarURL?.absoluteString,
            "email": self.email,
            "userHash": nil,
        ]
    }
}

extension UserSession {
    var dictionary: [String: Any?] {
        [
            "type": "authorized",
            "userId": String(self.userId.value),
            "accessToken": self.accessToken.value,
        ]
    }
}
