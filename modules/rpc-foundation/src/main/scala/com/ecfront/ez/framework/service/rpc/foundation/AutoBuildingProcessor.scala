package com.ecfront.ez.framework.service.rpc.foundation

import com.ecfront.common._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.annotation.StaticAnnotation
import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object AutoBuildingProcessor extends LazyLogging {

  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  /**
    * 使用基于注解的自动构建，此方法必须在服务启动“startup”后才能调用
    *
    * @param basePackage 服务类所在的根包名
    */
  def autoBuilding[A: TypeTag](basePackage: String, annClazz: Class[_ <: StaticAnnotation]): this.type = {
    ClassScanHelper.scan[RPC](basePackage).foreach {
      clazz =>
        if (clazz.getSimpleName.endsWith("$")) {
          process[A](runtimeMirror.reflectModule(runtimeMirror.staticModule(clazz.getName)).instance.asInstanceOf[AnyRef], annClazz)
        } else {
          process[A](clazz.newInstance().asInstanceOf[AnyRef], annClazz)
        }
    }
    this
  }

  def process[A: TypeTag](instance: AnyRef, annClazz: Class[_ <: StaticAnnotation]): Unit = {
    // 根路径
    var baseUri = BeanHelper.getClassAnnotation[RPC](instance.getClass).get.baseUri
    if (!baseUri.endsWith("/")) {
      baseUri += "/"
    }
    // 默认通道
    val defaultChannel = BeanHelper.getClassAnnotation[A](instance.getClass)
    // 存在指定通道的方法，当存在指定通道时默认无效
    val specialChannels = BeanHelper.findMethodAnnotations(instance.getClass, Seq(annClazz))

    BeanHelper.findMethodAnnotations(instance.getClass, Seq(classOf[GET], classOf[POST], classOf[PUT], classOf[DELETE])).foreach {
      methodInfo =>
        val methodName = methodInfo.method.name
        val methodMirror = BeanHelper.invoke(instance, methodInfo.method)
        val annInfo = methodInfo.annotation match {
          case ann: GET =>
            (Method.GET, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, null)
          case ann: POST =>
            (Method.POST, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
          case ann: PUT =>
            (Method.PUT, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, getClassFromMethodInfo(methodInfo))
          case ann: DELETE =>
            (Method.DELETE, if (ann.uri.startsWith("/")) ann.uri else baseUri + ann.uri, null)
        }
        if (specialChannels.contains(methodName) || defaultChannel.isDefined) {
          Router.add(annClazz.getSimpleName, annInfo._1, annInfo._2, annInfo._3, fun(annInfo._1, methodMirror))
        }
    }
  }

  def fun(method: String, methodMirror: universe.MethodMirror): (Map[String, String], Any, EZRPCContext) => Resp[Any] = {
    (parameter, body, context) =>
      try {
        if (method == Method.GET || method == Method.DELETE) {
          methodMirror(parameter, context).asInstanceOf[Resp[Any]]
        } else {
          methodMirror(parameter, body, context).asInstanceOf[Resp[Any]]
        }
      } catch {
        case e: Exception =>
          logger.error("Occurred unchecked exception", e)
          Resp.serverError(s"Occurred unchecked exception : ${e.getMessage}")
      }
  }

  private def getClassFromMethodInfo(methodInfo: methodAnnotationInfo): Class[_] = {
    BeanHelper.getClassByStr(methodInfo.method.paramLists.head(1).info.toString)
  }

}
