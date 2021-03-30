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
        String captchaId = "647f5ed2ed8acb4be36784e01556bb71";
        String captchaKey = "b09a7aafbfd83f73b35a9b530d0337bf";
        String domain = "http://gcaptcha4.geetest.com";

        // 2.获取用户验证后前端传过来的验证流水号等参数
        String lotNumber = getParams.get("lot_number");
        String captchaOutput = getParams.get("captcha_output");
        String passToken = getParams.get("pass_token");
        String genTime = getParams.get("gen_time");

        // 3.生成签名
        // 生成签名使用标准的hmac算法，使用用户当前完成验证的流水号lot_number作为原始消息message，使用客户验证私钥作为key
        // 采用sha256散列算法将message和key进行单向散列生成最终的签名
        String signToken = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, captchaKey).hmacHex(lotNumber);

        // 4.上传校验参数到极验二次验证接口, 校验用户验证状态
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("lot_number", lotNumber);
        queryParams.add("captcha_output", captchaOutput);
        queryParams.add("pass_token", passToken);
        queryParams.add("gen_time", genTime);
        queryParams.add("captcha_id", captchaId);
        queryParams.add("sign_token", signToken);

        String url = domain + "/validate";
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        JSONObject jsonObject = new JSONObject();
        //注意处理接口异常情况，当请求极验二次验证接口异常时做出相应异常处理
        //保证不会因为接口请求超时或服务未响应而阻碍业务流程
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
        JSONObject res = new JSONObject();
        if (jsonObject.getString("result").equals("success")) {
            res.put("login", "success");
        } else {
            res.put("login", "fail");
        }
        return res.toString();

    }
}
