项目概览:
wx-push 是一个基于 Spring Boot 2.7.3 的微信公众号模板消息定时推送服务，Java 8 编写。它的核心功能是每天定时（早上 8:30）向配置的微信用户推送一条包含天气、纪念日倒计时、生日倒计时、每日名言等信息的模板消息，同时也支持用户通过公众号发送城市名来实时切换天气查询城市。

项目结构与各层用途项目的包路径是 com.aguo.wxpush，按照经典的 Spring Boot 分层组织：
com.aguo.wxpush
├── WxPushApplication.java       ← 启动类
├── config/                      ← 配置层
│   └── WxConfigProperties.java
├── controller/                  ← 控制层（接口入口）
│   └── WxController.java
├── entity/                      ← 实体层
│   └── TextMessage.java
├── exception/                   ← 异常处理层
│   ├── GlobalExceptionHandler.java
│   └── WxPushException.java
├── service/                     ← 业务服务层（接口 + 实现）
│   ├── AccessTokenService.java
│   ├── MessageAssembler.java
│   ├── ProverbService.java      (接口)
│   ├── SendService.java         (接口)
│   ├── WeatherService.java      (接口)
│   └── impl/
│       ├── ProverbServiceImpl.java
│       ├── SendServiceImpl.java
│       └── WeatherServiceImpl.java
└── utils/                       ← 工具类层
├── DateUtil.java
├── HttpUtil.java
├── JsonObjectUtil.java
└── MessageUtil.java

各层详解：
1. 启动类 — WxPushApplication

标准 Spring Boot 入口，额外启用了 @EnableScheduling 注解以支持定时任务调度（@Scheduled）。

2. config 层 — 配置属性绑定

WxConfigProperties 通过 @ConfigurationProperties(prefix = "wx.config") 将 application.yml 中的配置项映射为 Java 对象，包含：微信公众号的 appId/appSecret、模板消息 ID、推送目标用户 openid 列表、天气 API 密钥、城市名、纪念日/生日日期、自定义消息文本、ApiSpace 的 token 等。这个类是全局的"配置中心"，几乎所有 Service 都依赖它。

3. controller 层 — 请求入口 + 定时调度

WxController 同时承担了两个角色：HTTP 接口和定时任务触发器。它暴露了四个端点：

GET /wx/send：手动触发消息推送，同时被 @Scheduled(cron = "0 30 8 ? * *") 标注，每天早上 8:30 自动执行。
GET /wx/changeConfig?city=xxx：通过网页表单修改天气查询城市，修改后立即触发一次推送。
GET /wx/receiveMsg：微信服务器接入验证（echostr 回传）。
POST /wx/receiveMsg：接收微信用户发来的文本消息，解析出城市名后切换城市并推送。
控制器里还有一个 normalizeCity() 私有方法，用于去除城市名中的"省/市/区/县"后缀。

4. entity 层 — 数据实体

TextMessage 是微信被动回复文本消息的实体模型，字段包括 toUserName、fromUserName、createTime、msgType、content，用于构建回复消息的 XML 结构。

5. exception 层 — 异常处理

WxPushException 是自定义业务异常类，继承 RuntimeException。GlobalExceptionHandler 使用 @RestControllerAdvice 做全局异常拦截，分别捕获 WxPushException 和通用 Exception，返回统一的 JSON 错误格式（code + msg）。

6. service 层 — 核心业务逻辑

这是项目最关键的层，包含五个服务：

AccessTokenService：微信 access_token 管理。采用双重检查锁（DCL）实现内存级缓存，token 有效期设为 7000 秒（官方 7200 秒提前刷新），避免每次推送都重新请求。通过微信 API https://api.weixin.qq.com/cgi-bin/token 获取。

MessageAssembler：消息组装服务，是推送内容的"大脑"。它协调调用 WeatherService 和 ProverbService 获取原始数据，然后按微信模板消息格式（每个字段由 value + color 组成的 JSONObject）组装出完整的 data Map。组装内容包括：日期星期头部、城市天气（当日 + 未来三天）、两个生日倒计时、纪念日年/月/天计数、每日名言（中英文）、自定义消息。

SendService / SendServiceImpl：推送调度中心。sendWeChatMsg() 编排了完整的推送流程——获取 token → 组装消息 → 遍历 openid 列表逐个发送模板消息 → 收集错误信息。messageHandle() 处理微信用户主动发来的消息，解析 XML 并提取文本内容。

WeatherService / WeatherServiceImpl：天气数据服务。调用一客天气 API（v1.yiketianqi.com）获取当日天气和未来一周天气，然后从周数据中提取今/明/后三天的天气描述。

ProverbService / ProverbServiceImpl：名言警句服务。提供两个来源：一个是免费的随机名言 API（api.xygeng.cn），另一个是需要注册的 ApiSpace 名言接口（更稳定可控）。还集成了有道翻译 API（fanyi.youdao.com）将中文名言翻译为英文。

7. utils 层 — 通用工具

HttpUtil：基于 OkHttp 封装的 HTTP 客户端，提供 GET、POST（JSON 格式）、POST（自定义 Header）三种请求方式，支持超时配置和连接重试。
DateUtil：日期工具类，基于 Java 8 java.time API，提供日期格式化、星期计算、天数差/月数差/年数差计算等方法。
JsonObjectUtil：简单的封装方法，将 value 和 color 打包为微信模板消息所需的 JSONObject 格式。
MessageUtil：微信消息 XML 解析工具。用 dom4j 的 SAXReader 解析微信回调请求中的 XML 数据，用 XStream 将 TextMessage 对象序列化为 XML。
8. 前端页面 — static/index.html

一个极简的 HTML 表单页面，用于通过浏览器手动输入城市名并提交到 /wx/changeConfig 接口，实现天气城市的在线切换。

核心业务流程
这个项目有三条核心业务链路：

流程一：每日定时推送（主流程）
这是最核心的流程，每天早上 8:30 由 Spring 的 @Scheduled 自动触发：
定时器触发 → WxController.send()
→ SendServiceImpl.sendWeChatMsg()
→ AccessTokenService.getAccessToken()     // 获取/缓存微信token
→ MessageAssembler.assembleTemplateData() // 组装消息内容
→ DateUtil 计算日期、星期
→ WeatherServiceImpl.getWeatherByCity()            // 获取当日天气
→ WeatherServiceImpl.getTheNextThreeDaysWeather()  // 获取未来三天天气
→ DateUtil 计算生日倒计时、纪念日天数
→ ProverbServiceImpl.getOneNormalProverb()          // 获取名言
→ ProverbServiceImpl.translateToEnglish()           // 翻译为英文
→ 遍历 openidList，对每个用户：
→ 构建模板消息JSON（touser, template_id, data）
→ HttpUtil.sendPost() 调用微信模板消息API
→ 汇总推送结果（成功/失败列表）

流程二：用户消息交互触发推送
用户在微信公众号中发送城市名称，服务器收到后切换城市并立即推送：
微信用户发送文本消息
→ 微信服务器 POST /wx/receiveMsg
→ SendServiceImpl.messageHandle()
→ MessageUtil.parseXml() 解析XML获取消息内容
→ WxController 中提取城市名 → normalizeCity() 去除后缀
→ wxConfig.setCity() 更新配置
→ SendServiceImpl.sendWeChatMsg()  // 立即触发一次完整推送

流程三：网页手动配置推送
通过浏览器访问静态页面修改城市：
用户访问 index.html → 填入城市名 → 提交表单
→ GET /wx/changeConfig?city=xxx
→ normalizeCity() 去除后缀
→ wxConfig.setCity() 更新配置
→ 页面返回"更新成功"提示
→ SendServiceImpl.sendWeChatMsg()  // 立即推送

总体来说，这是一个结构清晰、职责分明的小型微信推送服务。配置层集中管理所有外部参数，服务层通过 MessageAssembler 做数据聚合、SendServiceImpl 做流程编排，工具层提供 HTTP 通信和数据格式转换等基础能力，控制层同时承担定时调度和接口路由的角色。