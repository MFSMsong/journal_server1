package com.uuorb.journal.aop;

import cn.hutool.core.date.StopWatch;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.uuorb.journal.mapper.LogMapper;
import com.uuorb.journal.model.LogBean;
import com.uuorb.journal.util.IPUtil;
import com.uuorb.journal.util.TokenUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.Objects;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Resource
    LogMapper logMapper;

    private String header = "Authorization";

    @Pointcut("@annotation(com.uuorb.journal.annotation.Log)")
    public void log() {

    }

    @Around("log()")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String requestURI = request.getRequestURI();
        Object[] args = pjp.getArgs();
        String argStr = "";
        try {
            argStr = JSON.toJSONString(args);
        } catch (JSONException e) {
            argStr = "参数转换异常";
        }

        String userID = "";
        if (request.getHeader(header) != null) {
            String token = request.getHeader(header);
            userID = TokenUtil.getUserId(token);
        }

        String ipAddr = IPUtil.getIpAddr(request);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LogBean logBean = LogBean.builder()
            .url(requestURI)
            .createTime(new Date())
            .functionName(pjp.getSignature().getName())
            .params(argStr)
            .httpMethod(request.getMethod())
            .userID(userID)
            .ip(ipAddr)
            .build();
        
        try {
            Object proceed = pjp.proceed();
            stopWatch.stop();
            long totalTimeMillis = stopWatch.getTotalTimeMillis();
            logBean.setDuration(totalTimeMillis);
            logMapper.insertLog(logBean);
            return proceed;
        } catch (Throwable throwable) {
            log.error("方法执行异常: {}", throwable.getMessage(), throwable);
            throw throwable;
        }
    }
}
