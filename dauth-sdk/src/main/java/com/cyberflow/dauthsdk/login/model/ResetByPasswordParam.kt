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

/**
 * 
 * @param  
 * @param openudid 用户id
 * @param sign 检验参数
 */
data class ResetByPasswordParam (
    // 账号类型
    val user_type: Int,
    // 手机号,根据手机号重置密码时必填
    val phone: String? = null,
    // 手机区号,根据手机号重置密码时必填
    val phone_area_code: String? = null,
    // 邮箱账号,根据邮箱重置密码时必填
    val account: String? = null,
    // 密码,账号登录时必填
    val verify_code: String? = null,
    val password: String,
) {

}

