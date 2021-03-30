## 部署流程

1.配置极验参数

2.获取前端参数

3.生成签名

4.请求极验服务, 校验用户验证状态

5.根据极验返回的用户验证状态, 网站主进行自己的业务逻辑

## 二次校验接口
|接口信息|说明|
|---|---|
|接口地址|<http://gcaptcha4.geetest.com/validate>|
|请求方式|GET/POST|
|内容类型|application/x-www-form-urlencoded|
|返回格式|json|

1.请求参数
|参数名|类型|说明|
|---|---|---|
|lot_number|string|验证流水号|
|captcha_output|string|验证输出信息|
|pass_token|string|验证通过标识|
|gen_time|string|验证通过时间戳|
|captcha_id|string|验证 id|
|sign_token|string|验证签名|

2.响应参数
|参数名|类型|说明|
|---|---|---|
|result|string|二次校验结果|
|reason|string|校验结果说明|
|captcha_args|dict|验证输出参数|