package com.uuorb.journal.interceptor;

import com.alibaba.fastjson.JSON;
import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.constant.CacheConstant;
import com.uuorb.journal.constant.ResultStatus;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.util.RedisUtil;
import com.uuorb.journal.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            Authorization signature = hm.getMethodAnnotation(Authorization.class);
            if (signature == null) {
                return true;
            }

            String token = request.getHeader("Authorization");
            if (!StringUtil.isNullOrEmpty(token) && TokenUtil.validateToken(token)) {
                String userId = TokenUtil.getUserId(token);
                Object storedToken = redisUtil.get(CacheConstant.USER_TOKEN + userId);
                if (storedToken != null && token.equals(storedToken)) {
                    request.setAttribute("openid", TokenUtil.getUserOpenid(token));
                    return true;
                }
                log.warn("用户{}的Token与Redis中不匹配，可能已在其他设备登录", userId);
            }

            Result result = Result.error(ResultStatus.TOKEN_VALID);
            String strResponseJson = JSON.toJSONString(result);
            response.setContentType("application/json;charset=UTF-8");
            try (OutputStream out = response.getOutputStream()) {
                out.write(strResponseJson.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            return false;
        }

        return true;
    }
}
