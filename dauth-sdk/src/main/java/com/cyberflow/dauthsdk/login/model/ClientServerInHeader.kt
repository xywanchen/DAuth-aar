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
 * @param ClientID 所属应用id
 * @param ClientKey app标识key
 */

@Serializable
data class ClientServerInHeader (
    /* 所属应用id */
    val ClientID: String? = null,
    /* app标识key */
    val ClientKey: kotlin.String? = null
) {

}

