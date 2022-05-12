package com.geetest.demo.controller;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessagePack;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@Service
public class MainController {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @RequestMapping(value = "/login", method = {RequestMethod.GET})
    public String userLogin(@RequestParam Map<String, String> getParams) {

        // 配置参数
        String captchaId = "647f5ed2ed8acb4be36784e01556bb71";
        String captchaKey = "b09a7aafbfd83f73b35a9b530d0337bf";

        // 获取用户验证后前端传过来的验证流水号等参数
        String lotNumber = getParams.get("lot_number");
        String captchaOutput = getParams.get("captcha_output");
        String passToken = getParams.get("pass_token");
        String genTime = getParams.get("gen_time");

        // 二次校验流程
        String msg = captchaOutput + lotNumber + genTime;
        String tmpPassToken = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, captchaKey).hmacHex(msg);
        long t1 = Integer.parseInt(genTime);
        long t2 = System.currentTimeMillis() / 1000;
        int tmpTime = (int) (t2 - t1);
        ValueOperations ops = stringRedisTemplate.opsForValue();
        String cacheKey = "LOCAL_VALIDATE:" + lotNumber;
        Object cacheData = ops.get(cacheKey);
        JSONObject res = new JSONObject();
        if (!tmpPassToken.equals(passToken)) {
            // 验证口令不合法
            res.put("result", "fail");
        } else if (tmpTime > 600) {
            // 验证口令过期
            res.put("result", "fail");
        } else if (cacheData != null) {
            // 验证口令重复使用
            res.put("result", "fail");
        } else {
            // 验证成功
            res.put("result", "success");
            ops.set(cacheKey, passToken, 600, TimeUnit.SECONDS);
        }

        // 参数解析流程
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            String key = captchaKey.substring(0, 16);
            String iv = captchaKey.substring(16, 32);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(cipher.doFinal(Base64.decodeBase64(captchaOutput)));
            JSONObject captchaArgs = new JSONObject(unpacker.unpackValue().toJson());
            unpacker.close();
            // captchaArgs 为验证返回信息
            // System.out.println(captchaArgs);
            // String user_ip = res.getString("user_ip");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res.toString();


    }
}
