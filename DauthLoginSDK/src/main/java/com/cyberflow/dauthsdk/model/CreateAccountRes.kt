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

import com.cyberflow.dauthsdk.network.BaseResponse


/**
 * 
 * @param SessionID 登录凭证
 */
data class CreateAccountRes(val data: Data) : BaseResponse() {
    class Data {
        /* 登录凭证 */
        var SessionID: String? = null
    }

}
