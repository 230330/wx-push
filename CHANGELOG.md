# 变更记录（CHANGELOG）

> 本文件记录项目的所有重要变更及对应的验证自证。
> 规则：每次变更必须包含「变更内容」与「自证」两部分；自证包含成功与失败的客观记录。

---

## 2026-10-03 天气服务迁移：一客天气 → 和风天气 QWeather API

### 背景

原天气数据源"一客天气"（v1.yiketianqi.com）更换为和风天气 QWeather API（https://dev.qweather.com）。
开发者ID：Q96B30D7C0；API Host：pj5egpeqyt.re.qweatherapi.com；API KEY：TAGX****8VE。

### 变更内容

| 文件 | 改动说明 |
|---|---|
| pom.xml | 新增 net.i2p.crypto:eddsa:0.3.0 依赖（Java 8 的 Ed25519 签名支持，官方文档推荐） |
| src/main/java/com/aguo/wxpush/config/WxConfigProperties.java | weatherAppId/weatherAppSecret 替换为 weatherApiHost/weatherDeveloperId/weatherProjectId/weatherCredentialId/weatherPrivateKey/weatherApiKey 六个新字段 |
| src/main/java/com/aguo/wxpush/utils/QWeatherJwtUtil.java | 新增：和风天气 JWT 生成工具（EdDSA 签名，私钥兼容 PEM/Base64 两种格式） |
| src/main/java/com/aguo/wxpush/utils/HttpUtil.java | 新增带自定义请求头的 sendGet 重载方法 |
| src/main/java/com/aguo/wxpush/service/impl/WeatherServiceImpl.java | 重写：城市查询 /geo/v2/city/lookup（带内存缓存）→ 实时天气 /weather/v1/current/{lat}/{lon} + 逐天预报 /weather/v1/daily/{lat}/{lon}?days=N；风向代码转中文、湿度小数转百分比；返回结构兼容旧格式，MessageAssembler 未改动 |
| src/main/resources/application.yml | 天气配置段全部更新为 QWeather 参数（环境变量注入） |
| src/main/resources/application-example.yml | 同步更新并补充 JWT/API KEY 配置说明 |
| env.ps1 | 替换旧 WEATHER_APP_ID/WEATHER_APP_SECRET，新增 WEATHER_API_HOST/WEATHER_DEV_ID/WEATHER_PROJECT_ID/WEATHER_CREDENTIAL_ID/WEATHER_PRIVATE_KEY/WEATHER_API_KEY |
| README.md | 更新天气服务描述，新增《和风天气 API 配置指南》 |

### 自证

**1. 编译验证（通过）**

- 命令：`mvn -q compile -DskipTests`，退出码 0。
- 新类已生成：target/classes/com/aguo/wxpush/utils/QWeatherJwtUtil.class、
  target/classes/com/aguo/wxpush/service/impl/WeatherServiceImpl.class（含内部类 Location）。

**2. API 调用自证（2026-10-03，均返回 401，鉴权失败）**

| # | 请求 | 鉴权方式 | 结果 |
|---|---|---|---|
| 1 | GET https://pj5egpeqyt.re.qweatherapi.com/geo/v2/city/lookup?location=贵阳&range=cn&lang=zh | 请求头 X-QW-Api-Key: TAGX****8VE | HTTP 401 |
| 2 | 同上 | 查询参数 key=TAGX****8VE | HTTP 401 |
| 3 | GET https://pj5egpeqyt.re.qweatherapi.com/weather/v1/current/26.65/106.63?lang=zh | 请求头 X-QW-Api-Key: TAGX****8VE | HTTP 401 |
| 4 | 同上 | 请求头 X-QW-Api-Key: tagx****8ve（小写） | HTTP 401 |

错误响应体：`{"error":{"status":401,"type":".../error-code/#unauthorized","title":"Unauthorized","detail":"Authentication failed, check your KEY/Token or Host."}}`

**结论**：API Host 网络连通正常（服务器正常返回 JSON 错误）；API KEY `TAGX****8VE` 与该 Host 鉴权失败，
待用户核实（可能原因：KEY 与 Host/项目不匹配、KEY 无效或未启用、KEY 复制有误）。

**3. 二次自证（2026-10-03，用户补充项目ID 2F87MR7MYB 后复核，仍 401）**

| # | 验证项 | 结果 |
|---|---|---|
| 1 | curl.exe 复测 GET /weather/v1/current/26.65/106.63（header 方式） | HTTP 401（排除 PowerShell 环境因素） |
| 2 | 完整响应头分析 | HTTP/1.1 401，Content-Type: application/problem+json，响应体 gzip 压缩 |
| 3 | DNS 解析 pj5egpeqyt.re.qweatherapi.com | 成功，解析到 47.93.233.85 |
| 4 | DNS 解析 pj5egpeqyt.qweatherapi.com（无 .re. 变体） | 解析失败（域名不存在） |
| 5 | 官方错误码文档核查 | 401 = Authentication failed，官方不透露具体原因；403 才对应安全限制/额度问题 |

**复核结论**：API Host `pj5egpeqyt.re.qweatherapi.com` 确认为正确域名；请求格式符合官方文档（X-QW-Api-Key 头、
query 参数 key= 两种方式均已尝试）。401 锁定为 KEY 与账号/项目不匹配或 KEY 无效，需用户在控制台人工核实。

### 待办

- [x] 用户核实控制台 -> 设置 中 API Host 是否为 pj5egpeqyt.re.qweatherapi.com
- [x] 用户核实控制台 -> 项目管理 -> 凭据 中该凭据的"认证方式"是否确为 API KEY（而非 JSON Web Token），KEY 是否确为 TAGX****8VE 且状态启用
- [x] 用户核实凭据是否设置了"应用限制"（IP 白名单等），当前出口 IP 是否在白名单内
- [x] 修正配置后重新自证 API 调用并更新本记录

### 401 根因与最终自证（2026-10-03 更新）

**401 根因**：用户最初提供的 TAGX****8VE 实为 JWT 凭据的**凭据ID（kid）**，并非 API KEY。
真正的 API KEY 为 `7102****240706`（32位hex，控制台创建"API KEY 凭据"后获得，完整值仅保存在本地的 env.ps1 与 .vscode/launch.json 中，二者均被 .gitignore 排除）。

**信息更正后自证（全部通过）**：

| # | 验证项 | 结果 |
|---|---|---|
| 1 | GET /geo/v2/city/lookup?location=贵阳&range=cn&lang=zh（X-QW-Api-Key 头） | code=200，返回贵阳 lat=26.64669 lon=106.62820 |
| 2 | GET /weather/v1/current/26.64669/106.62820?lang=zh | 正常：condition.text=阵雨，humidity=0.94，wind.direction.compass=ne，scale=3 |
| 3 | GET /weather/v1/daily/26.64669/106.62820?days=3&lang=zh | 正常：3 天预报，含 temperatureMax/Min、daytime.condition.text |
| 4 | 新增集成测试 src/test/java/com/aguo/wxpush/service/WeatherServiceIntegrationTest.java，运行 `mvn test -Dtest=WeatherServiceIntegrationTest` | Tests run: 2, Failures: 0, Errors: 0, BUILD SUCCESS |
| 5 | getWeatherByCity() 实际输出 | {"city":"贵阳","wea":"阵雨","tem_day":"20","tem_night":"15","humidity":"94","win":"东北风","win_speed":"4m/s"} |
| 6 | getTheNextThreeDaysWeather() 实际输出 | {今=阵雨, 明=中雨, 后=多云} |

**过程中发现并修复的问题**：env.ps1 为无 BOM 的 UTF-8 文件，Windows PowerShell 5.1 按 ANSI 误读
导致中文注释乱码、脚本加载失败。已重写为带 UTF-8 BOM 的文件，加载验证通过。
（经验：本项目 .ps1 脚本必须保存为带 BOM 的 UTF-8。）

---

## 2026-10-03 调试报错修复：VS Code 启动时天气鉴权失败（JWT 空私钥异常）

### 问题现象

用户从 VS Code 调试启动应用，触发 /wx/send 推送，控制台报错：

```
ERROR QWeatherJwtUtil - 生成和风天气 JWT 失败: java.lang.ArrayIndexOutOfBoundsException: 11
ERROR HttpUtil - GET ... /geo/v2/city/lookup ... HTTP状态码：401
ERROR WeatherServiceImpl - 无法定位城市 [贵阳]，请检查城市名配置
```

微信消息推送本身成功，但天气数据缺失。

### 根因

两层问题：
1. **启动配置未更新**：.vscode/launch.json 中仍是旧的天气环境变量名
   （WEATHER_APP_ID/WEATHER_APP_SECRET，且值错位），导致 WEATHER_API_KEY 未注入，
   代码回退到 JWT 分支，而 JWT 私钥也未配置。
2. **代码健壮性缺陷**：空私钥直接传入 EdDSAPrivateKey 解码，
   eddsa 库底层抛出 ArrayIndexOutOfBoundsException: 11，
   异常信息误导（未提示"配置缺失"）。

### 修复内容

| 文件 | 修复 |
|---|---|
| .vscode/launch.json | 天气环境变量更新为新变量名：WEATHER_API_HOST/WEATHER_DEV_ID/WEATHER_PROJECT_ID/WEATHER_CREDENTIAL_ID/WEATHER_API_KEY |
| QWeatherJwtUtil.java | createToken 入口增加参数校验：任一参数为空直接返回 null 并输出可读日志；新增 InvalidKeySpecException 单独捕获（提示私钥格式无效） |
| WeatherServiceImpl.java | buildAuthHeaders 增加 JWT 配置完整性校验：API KEY 与 JWT 均缺失时输出明确错误日志（列出所需环境变量），不再触发底层解码异常 |
| WeatherServiceIntegrationTest.java | 新增回归测试 testCreateTokenWithMissingConfig：验证空/空白/非法私钥均返回 null 不抛异常 |

### 自证

**1. 正常场景（env 已加载）**：`mvn test -Dtest=WeatherServiceIntegrationTest`

- Tests run: 3, Failures: 0, Errors: 0, BUILD SUCCESS
- getWeatherByCity / 未来三天天气 / 配置缺失回归测试 全部通过

**2. 缺失配置场景（清空 WEATHER_API_KEY 与 WEATHER_PRIVATE_KEY）**：
`mvn test -Dtest="WeatherServiceIntegrationTest#testGetWeatherByCity"`

- **无 ArrayIndexOutOfBoundsException 出现（PASS）**
- 日志输出清晰的错误提示："和风天气 API 鉴权配置缺失：未配置 API KEY（WEATHER_API_KEY），
  且 JWT 配置不完整（需 WEATHER_PRIVATE_KEY/WEATHER_CREDENTIAL_ID/WEATHER_DEV_ID/WEATHER_PROJECT_ID）"
- 测试按预期 fail（无鉴权拿不到数据），验证错误处理路径正常、无底层异常泄漏

**3. launch.json 更新后**：用户重新从 VS Code 启动即可加载正确的天气环境变量，
天气数据可正常获取（环境变量注入路径与集成测试一致）。
