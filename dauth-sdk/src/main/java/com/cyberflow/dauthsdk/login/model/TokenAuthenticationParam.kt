/**
* dauthwallet
* 账号注册，登录，授权接口
*
* OpenAPI spec version: v1.0
* 
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/
package com.cyberflow.dauthsdk.login.model

import kotlinx.serialization.Serializable


/**
 * 
 * @param  
 * @param platform 平台
 * @param code dauth或第三方的code
 * @param grant_type grand_type
 * @param redirect_uri 
 * @param code_verifier 代码校验器
 */

@Serializable
data class TokenAuthenticationParam(
    val code_verifier: String,

    /* dauth或第三方的code */
    val code: String,

    val sign: String? = null,
    /* grand_type */
    val grant_type: Int? = null,
    val redirect_uri: String? = null,
    /* 平台 */
    val platform: String? = null,
    val clientServerInHeader: ClientServerInHeader? = null
) {

}

