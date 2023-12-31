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
package com.infras.dauthsdk.login.model



/**
 * 
 * @param  
 * @param openudid 用户id
 * @param user_type 账号类型
 * @param platform 平台来源
 * @param refresh_token 用于刷新access_token
 * @param sign 检验参数
 */

data class RefreshTokenParam (
    /* 用户id */
    val authid: String,
    /* 账号类型 */
    val user_type: Int,
    /* 用于刷新access_token */
    val refresh_token: String,
    val clientInHeader: ClientInHeader? = null
) {

}

