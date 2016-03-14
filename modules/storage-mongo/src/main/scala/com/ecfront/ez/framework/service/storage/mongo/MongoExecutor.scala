package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.storage.foundation.BaseModel
import io.vertx.core.json.{JsonArray, JsonObject}


object MongoExecutor {

  def save[M](entityInfo: MongoEntityInfo, collection: String, save: JsonObject, clazz: Class[M]): Resp[M] = {
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = new JsonArray()
      entityInfo.uniqueFieldNames.filter(save.containsKey).foreach {
        field =>
          existQuery.add(new JsonObject().put(field, save.getValue(field)))
      }
      val existR = MongoProcessor.exist(collection, new JsonObject().put("$or", existQuery))
      if (existR) {
        if (existR.body) {
          Resp.badRequest(entityInfo.uniqueFieldNames.map {
            field =>
              if (entityInfo.fieldLabel.contains(field)) {
                entityInfo.fieldLabel(field)
              } else {
                field
              }
          }.mkString("[", ",", "]") + " must be unique")
        } else {
          val saveR = MongoProcessor.save(collection, save)
          if (saveR) {
            MongoProcessor.getById(collection, saveR.body, clazz)
          } else {
            saveR
          }
        }
      } else {
        existR
      }
    } else {
      val saveR = MongoProcessor.save(collection, save)
      if (saveR) {
        MongoProcessor.getById(collection, saveR.body, clazz)
      } else {
        saveR
      }
    }
  }

  def update[M](entityInfo: MongoEntityInfo, collection: String, id: String, update: JsonObject, clazz: Class[M]): Resp[M] = {
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = new JsonObject()
      entityInfo.uniqueFieldNames.filter(update.containsKey).foreach {
        field =>
          existQuery.put(field, update.getValue(field))
      }
      existQuery.put("_id", new JsonObject().put("$ne", id))
      val existR = MongoProcessor.exist(collection, existQuery)
      if (existR) {
        if (existR.body) {
          Resp.badRequest(entityInfo.uniqueFieldNames.map {
            field =>
              if (entityInfo.fieldLabel.contains(field)) {
                entityInfo.fieldLabel(field)
              } else {
                field
              }
          }.mkString("[", ",", "]") + " must be unique")
        } else {
          val updateR = MongoProcessor.update(collection, id, update)
          if (updateR) {
            MongoProcessor.getById(collection, updateR.body, clazz)
          } else {
            updateR
          }
        }
      } else {
        existR
      }
    } else {
      val updateR = MongoProcessor.update(collection, id, update)
      if (updateR) {
        MongoProcessor.getById(collection, updateR.body, clazz)
      } else {
        updateR
      }
    }
  }

  def saveOrUpdate[M](entityInfo: MongoEntityInfo, collection: String, id: String, saveOrUpdate: JsonObject, clazz: Class[M]): Resp[M] = {
    if (saveOrUpdate.getValue(BaseModel.Id_FLAG) == null) {
      save(entityInfo, collection, saveOrUpdate, clazz)
    } else {
      update(entityInfo, collection, saveOrUpdate.getString(BaseModel.Id_FLAG), saveOrUpdate, clazz)
    }
  }

}
