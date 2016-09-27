package com.ecfront.ez.framework.service.email

import com.ecfront.ez.framework.core.test.MockStartupSpec

class EMailSpec extends MockStartupSpec {

  test("Email Test") {
    val sendResp = EmailProcessor.send(
      "测试用户1", "364341806@qq.com",
      "测试邮件",
      "<h1>测试邮件</h1><br/>1\r\n2\r\n",
      List(("测试附件", this.getClass.getResource("/").getPath + "ez.json"))
    )
    assert(sendResp)
  }
}


