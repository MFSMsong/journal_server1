package com.uuorb.journal.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.Log;
import com.uuorb.journal.annotation.UserId;
import com.uuorb.journal.constant.CacheConstant;
import com.uuorb.journal.constant.ResultStatus;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.model.User;
import com.uuorb.journal.service.UserService;
import com.uuorb.journal.util.EmailUtil;
import com.uuorb.journal.util.IPUtil;
import com.uuorb.journal.util.RedisUtil;
import com.uuorb.journal.util.SMSUtil;
import com.uuorb.journal.util.TokenUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    RedisUtil redisUtil;

    @Resource
    private UserService userService;

    @Autowired
    SMSUtil smsUtil;

    @Autowired
    EmailUtil emailUtil;

    @Log
    @Authorization
    @GetMapping("/profile/me")
    Result<User> getSelfProfile(@UserId String userId) {
        return Result.ok(userService.getUserByUserId(userId));
    }

    @PostMapping("/login/smsCode")
    Result sendSmsCode(@RequestParam("telephone") String telephone, HttpServletRequest request) {
        log.info("发送手机验证码:{}", telephone);
        boolean isMobile = Validator.isMobile(telephone);
        if (!isMobile) {
            return Result.error(ResultStatus.TELEPHONE_ERROR);
        }

        // 通过request获取ip
        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        // 生成4位数字
        String code = RandomUtil.randomNumbers(4);

        // 5分钟有效
        redisUtil.set(CacheConstant.LOGIN_CODE + telephone, code, 5 * 60);
        smsUtil.sendLoginMsg(telephone, code);

        log.info("登陆验证码:{},{}", telephone, ipAddr);
        return Result.ok();
    }

    @PostMapping("/login/emailCode")
    Result sendEmailCode(@RequestParam("email") String email, HttpServletRequest request) {
        log.info("发送邮箱验证码:{}", email);
        boolean isEmail = Validator.isEmail(email);
        if (!isEmail) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        // 通过request获取ip
        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        // 生成4位数字
        String code = RandomUtil.randomNumbers(4);

        // 5分钟有效
        redisUtil.set(CacheConstant.LOGIN_CODE + email, code, 5 * 60);
        emailUtil.sendLoginCode(email, code);

        log.info("登陆验证码:{},{}", email, ipAddr);
        return Result.ok();
    }

    @PostMapping("/login")
    Result login(@RequestParam("account") String account, @RequestParam(name = "code") String code) {
        boolean isMobile = Validator.isMobile(account);
        boolean isEmail = Validator.isEmail(account);

        if (!isMobile && !isEmail) {
            return Result.error(ResultStatus.ACCOUNT_ERROR);
        }
        String userId;

        // 校验 code
        Object o = redisUtil.get(CacheConstant.LOGIN_CODE + account);
        if (o == null || !o.toString().equalsIgnoreCase(code)) {
            return Result.error(ResultStatus.VERIFY_CODE_ERROR);
        }

        // 删除code
        redisUtil.del(CacheConstant.LOGIN_CODE + account);

        // 先查，如果存在，则返回token，否则注册
        if (isMobile) {
            List<User> userList = userService.selectUserByPhone(account);
            if (CollectionUtil.isNotEmpty(userList)) {
                userId = userList.get(0).getUserId();
            } else {
                User user = userService.registerByPhone(account);
                userId = user.getUserId();
            }
        } else {
            List<User> userList = userService.selectUserByEmail(account);
            if (CollectionUtil.isNotEmpty(userList)) {
                userId = userList.get(0).getUserId();
            } else {
                User user = userService.registerByEmail(account);
                userId = user.getUserId();
            }
        }

        String token = TokenUtil.generateToken(userId);
        return Result.ok(token);
    }

    @PostMapping("/login/wechat")
    Result loginWechat(@RequestParam("code") String code, @RequestParam("platform") String platform) {
        String token = userService.loginWithWechat(code, platform);
        return Result.ok(token);
    }

    @PostMapping("/login/apple")
    Result loginWithApple(@RequestParam("code") String code) {
        String token = null;
        try {
            token = userService.loginWithApple(code);
            return Result.ok(token);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Result.error();
        }
    }

    @Log
    @Authorization
    @PatchMapping
    Result updateUser(@UserId String userId, @RequestBody User user) {
        user.setUserId(userId);
        userService.updateUser(user);
        return Result.ok();
    }

    @Log
    @Authorization
    @DeleteMapping("/delete")
    Result deleteUser(@UserId String userId) {
        userService.deleteUser(userId);
        return Result.ok();
    }
}
