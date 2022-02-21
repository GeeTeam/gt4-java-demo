package com.geetest.demo.controller;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class MainController {


    @RequestMapping(value = "/login", method = {RequestMethod.GET})
    public String userLogin(@RequestParam Map<String, String> getParams) {
        // 1.初始化极验参数信息
        // 1.initialize geetest parameter
        String captchaId = "647f5ed2ed8acb4be36784e01556bb71";
        String captchaKey = "b09a7aafbfd83f73b35a9b530d0337bf";
        String domain = "http://gcaptcha4.geetest.com";

        // 2.获取用户验证后前端传过来的验证流水号等参数
        // 2.get the verification parameters passed from the front end after verification
        String lotNumber = getParams.get("lot_number");
        String captchaOutput = getParams.get("captcha_output");
        String passToken = getParams.get("pass_token");
        String genTime = getParams.get("gen_time");

        // 3.生成签名
        // 3.generate signature
        // 生成签名使用标准的hmac算法，使用用户当前完成验证的流水号lot_number作为原始消息message，使用客户验证私钥作为key
        // use standard hmac algorithms to generate signatures, and take the user's current verification serial number lot_number as the original message, and the client's verification private key as the key
        // 采用sha256散列算法将message和key进行单向散列生成最终的签名
        // use sha256 hash algorithm to hash message and key in one direction to generate the final signature
        String signToken = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, captchaKey).hmacHex(lotNumber);

        // 4.上传校验参数到极验二次验证接口, 校验用户验证状态
        // 4.upload verification parameters to the secondary verification interface of GeeTest to validate the user verification status
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("lot_number", lotNumber);
        queryParams.add("captcha_output", captchaOutput);
        queryParams.add("pass_token", passToken);
        queryParams.add("gen_time", genTime);
        queryParams.add("sign_token", signToken);
        // captcha_id 参数建议放在 url 后面, 方便请求异常时可以在日志中根据id快速定位到异常请求
        // geetest recommends to put captcha_id parameter after url, so that when a request exception occurs, it can be quickly located in the log according to the id
        String url = String.format(domain + "/validate" + "?captcha_id=%s", captchaId);
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        JSONObject jsonObject = new JSONObject();
        //注意处理接口异常情况，当请求极验二次验证接口异常时做出相应异常处理
        // pay attention to interface exceptions, and make corresponding exception handling when requesting GeeTest secondary verification interface exceptions or response status is not 200
        //保证不会因为接口请求超时或服务未响应而阻碍业务流程
        // website's business will not be interrupted due to interface request timeout or server not-responding
        try {
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(queryParams, headers);
            ResponseEntity<String> response = client.exchange(url, method, requestEntity, String.class);
            String resBody = response.getBody();
            jsonObject = new JSONObject(resBody);
        }catch (Exception e){
            jsonObject.put("result","success");
            jsonObject.put("reason","request geetest api fail");
        }

        // 5.根据极验返回的用户验证状态, 网站主进行自己的业务逻辑
        // 5. taking the user authentication status returned from geetest into consideration, the website owner follows his own business logic
        JSONObject res = new JSONObject();
        if (jsonObject.getString("result").equals("success")) {
            res.put("login", "success");
            res.put("reason", jsonObject.getString("reason"));
        } else {
            res.put("login", "fail");
            res.put("reason", jsonObject.getString("reason"));
        }
        return res.toString();

    }
}
