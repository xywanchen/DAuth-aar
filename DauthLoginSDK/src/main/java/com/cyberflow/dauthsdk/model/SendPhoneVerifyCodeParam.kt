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

import com.cyberflow.dauthsdk.model.AccessTokenInHeader

/**
 * 
 * @param  
 * @param openudid 用户id
 * @param phone 手机号
 * @param phone_area_code 手机区号
 * @param sign 检验参数
 */
data class SendPhoneVerifyCodeParam (
    /* 用户id */
    val openudid: String,
    /* 手机号 */
    val phone: String,
    /* 手机区号 */
    val phone_area_code: String,
    /* 检验参数 */
    val sign: String,
     val accessTokenInHeader: AccessTokenInHeader? = null
) {

}

