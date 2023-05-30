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
 * @param openudid 用户openudid
 * @param user_type 账号类型
 * @param sign 检验参数
 */
data class GetBindAcoountsParam (
    /* 用户openudid */
    val openudid: String,
    /* 账号类型 */
    val user_type: Int,
    /* 检验参数 */
    val sign: String,
     val accessTokenInHeader: AccessTokenInHeader? = null
) {

}
