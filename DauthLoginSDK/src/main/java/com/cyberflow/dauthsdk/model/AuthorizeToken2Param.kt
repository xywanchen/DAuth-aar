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
package com.cyberflow.dauthsdk.model

import com.cyberflow.dauthsdk.login.DAuthUser


/**
 * 
 * @param  
 * @param access_token access_token
 * @param refresh_token refresh_token
 * @param id_token id_token
 * @param user_type 登录来源
 * @param sign 
 */
data class AuthorizeToken2Param (
    /* access_token */
    val access_token: kotlin.String? = null,
    /* refresh_token */
    val refresh_token: kotlin.String? = null,
    /* 登录来源 */
    val user_type: kotlin.Long,
    val sign: kotlin.String,
    val commonHeader: CommonHeader? = null,
    /* id_token */
    val id_token: kotlin.String? = null,
    val user_data: String? = null
) {

}

