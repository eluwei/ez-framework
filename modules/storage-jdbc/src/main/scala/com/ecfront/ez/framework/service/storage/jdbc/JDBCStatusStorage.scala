package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation.{EZStorageContext, StatusModel, StatusStorage}

trait JDBCStatusStorage[M <: StatusModel] extends JDBCBaseStorage[M] with StatusStorage[M] {

  override def doEnableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond(" enable = true ", " id = ?", List(id), context)
  }

  override def doDisableById(id: Any, context: EZStorageContext): Resp[Void] = {
    doUpdateByCond(" enable = false ", " id = ?", List(id), context)
  }

  override protected def appendEnabled(condition: String): String = {
    condition + " AND enable = true "
  }

}







