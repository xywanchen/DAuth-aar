package com.infras.dauthsdk.login.model

class CheckEmailParam(
    private val account: String,
    private val verify_code: String
) : IAccessTokenRequest, IAuthorizationRequest