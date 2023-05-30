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

 

/**
 * 
 * @param  
 * @param user_type 账号类型
 * @param scope 
 * @param redirect_uri 
 * @param response_type 
 * @param state 
 * @param code_challenge 
 * @param code_challenge_method 
 */
data class AuthorizeParam (
    /* 账号类型 */
    val user_type: Int,
    val scope: String,
    val redirect_uri: String,
    val response_type: String,
    val state: String,
    val code_challenge: String,
    val code_challenge_method: String,
    val clientInHeader : ClientInHeader? = null
) {

}
