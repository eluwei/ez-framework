package com.ecfront.ez.framework.service.auth

import com.ecfront.common.AsyncResp
import com.ecfront.ez.framework.service.auth.model.{EZ_Token_Info, _}
import com.ecfront.ez.framework.service.rpc.foundation.EZRPCContext
import com.ecfront.ez.framework.service.rpc.http.HttpInterceptor
import com.ecfront.ez.framework.service.storage.jdbc.JDBCProcessor
import com.ecfront.ez.framework.service.storage.mongo.MongoProcessor
import io.vertx.core.json.JsonObject

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Http 权限拦截器
  */
object AuthHttpInterceptor extends HttpInterceptor {

  override def before(obj: EZRPCContext, context: mutable.Map[String, Any], p: AsyncResp[EZRPCContext]): Unit = {
    val authContext: EZAuthContext = obj
    authContext.token =
      if (authContext.parameters.contains(EZ_Token_Info.TOKEN_FLAG)) {
        Some(authContext.parameters(EZ_Token_Info.TOKEN_FLAG))
      } else {
        None
      }
    if (authContext.templateUri.startsWith(ServiceAdapter.publicUriPrefix)) {
      // 可匿名访问
      p.success(authContext)
    } else {
      if (authContext.token.isEmpty) {
        p.unAuthorized(s"【token】not exist，Request parameter must include【${EZ_Token_Info.TOKEN_FLAG}】")
      } else {
        val tokenF =
          if (ServiceAdapter.mongoStorage) {
          MongoProcessor.Async.getById(EZ_Token_Info_Mongo.tableName, authContext.token.get, classOf[EZ_Token_Info])
        } else {
          JDBCProcessor.Async.get(
            s"SELECT * FROM ${EZ_Token_Info_JDBC.tableName} WHERE id = ?",
            List(authContext.token.get), classOf[EZ_Token_Info])
        }
        tokenF.onSuccess {
          case tokenR =>
            if (tokenR && tokenR.body != null) {
              authContext.loginInfo = Some(tokenR.body)
              val resourceCode = EZ_Resource.assembleCode(authContext.method, authContext.templateUri)
              if (!tokenR.body.roles.exists(_.resource_codes.contains(resourceCode))) {
                val resF = if (ServiceAdapter.mongoStorage) {
                  MongoProcessor.Async.exist(EZ_Resource_Mongo.tableName, new JsonObject(s"""{"code":"$resourceCode"}"""))
                } else {
                  JDBCProcessor.Async.exist(
                    s"SELECT * FROM ${EZ_Resource_Mongo.tableName} WHERE code = ?",
                    List(resourceCode))
                }
                resF.onSuccess {
                  case resR =>
                    if (resR && !resR.body) {
                      // 所有登录用户都可以访问
                      p.success(authContext)
                    } else {
                      p.unAuthorized(s"Account【${tokenR.body.login_name}】no access to ${authContext.realUri}】")
                    }
                }
              } else {
                p.success(authContext)
              }
            } else {
              p.unAuthorized("【token】not exist")
            }
        }
      }
    }
  }

  override def after(obj: EZRPCContext, context: mutable.Map[String, Any], p: AsyncResp[EZRPCContext]): Unit = {
    p.success(obj)
  }

}
