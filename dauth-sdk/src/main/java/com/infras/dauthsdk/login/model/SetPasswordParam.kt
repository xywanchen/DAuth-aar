package com.infras.dauthsdk.login.model


class SetPasswordParam : IAccessTokenRequest, IAuthorizationRequest {
    var password: String? = null
    var old_password: String? = null
}