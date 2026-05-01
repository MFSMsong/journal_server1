package com.uuorb.journal.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
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
import java.util.regex.Pattern;

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

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$");

    @Log
    @Authorization
    @GetMapping("/profile/me")
    Result<User> getSelfProfile(@UserId String userId) {
        return Result.ok(userService.getUserByUserId(userId));
    }

    @PostMapping("/login/emailCode")
    Result sendEmailCode(@RequestParam("email") String email, HttpServletRequest request) {
        log.info("发送邮箱验证码:{}", email);
        boolean isEmail = Validator.isEmail(email);
        if (!isEmail) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        String code = RandomUtil.randomNumbers(4);
        redisUtil.set(CacheConstant.LOGIN_CODE + email, code, 5 * 60);
        emailUtil.sendLoginCode(email, code);

        log.info("登陆验证码:{},{}", email, ipAddr);
        return Result.ok();
    }

    @PostMapping("/register/emailCode")
    Result sendRegisterEmailCode(@RequestParam("email") String email, HttpServletRequest request) {
        log.info("发送注册邮箱验证码:{}", email);
        boolean isEmail = Validator.isEmail(email);
        if (!isEmail) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        List<User> userList = userService.selectUserByEmail(email);
        if (CollectionUtil.isNotEmpty(userList)) {
            return Result.error(ResultStatus.EMAIL_REGISTERED);
        }

        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        String code = RandomUtil.randomNumbers(4);
        redisUtil.set(CacheConstant.REGISTER_CODE + email, code, 5 * 60);
        emailUtil.sendLoginCode(email, code);

        log.info("注册验证码:{},{}", email, ipAddr);
        return Result.ok();
    }

    @Log
    @Authorization
    @PostMapping("/password/emailCode")
    Result sendPasswordEmailCode(@UserId String userId, HttpServletRequest request) {
        log.info("发送修改密码邮箱验证码:{}", userId);
        
        User user = userService.getUserByUserId(userId);
        String email = user.getEmail();
        
        if (email == null || email.isEmpty()) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        String code = RandomUtil.randomNumbers(4);
        redisUtil.set(CacheConstant.PASSWORD_CODE + email, code, 5 * 60);
        emailUtil.sendLoginCode(email, code);

        log.info("修改密码验证码:{},{}", email, ipAddr);
        return Result.ok();
    }

    @Log
    @Authorization
    @PostMapping("/delete/emailCode")
    Result sendDeleteAccountEmailCode(@UserId String userId, HttpServletRequest request) {
        log.info("发送删除账户邮箱验证码:{}", userId);
        
        User user = userService.getUserByUserId(userId);
        String email = user.getEmail();
        
        if (email == null || email.isEmpty()) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        String ipAddr = IPUtil.getIpAddr(request);
        String key = CacheConstant.SEND_SMS_IP + ipAddr;
        Long count = redisUtil.incr(key, 1);

        if (count.equals(1L)) {
            redisUtil.expire(key, 60);
        }

        if (count > 1) {
            return Result.error(ResultStatus.VERIFY_CODE_LIMITED);
        }

        String code = RandomUtil.randomNumbers(4);
        redisUtil.set(CacheConstant.DELETE_ACCOUNT_CODE + email, code, 5 * 60);
        emailUtil.sendLoginCode(email, code);

        log.info("删除账户验证码:{},{}", email, ipAddr);
        return Result.ok();
    }

    @PostMapping("/login")
    Result login(@RequestParam("account") String account, @RequestParam(name = "code") String code) {
        boolean isEmail = Validator.isEmail(account);

        if (!isEmail) {
            return Result.error(ResultStatus.ACCOUNT_ERROR);
        }
        String userId;

        Object o = redisUtil.get(CacheConstant.LOGIN_CODE + account);
        if (o == null || !o.toString().equalsIgnoreCase(code)) {
            return Result.error(ResultStatus.VERIFY_CODE_ERROR);
        }

        redisUtil.del(CacheConstant.LOGIN_CODE + account);

        List<User> userList = userService.selectUserByEmail(account);
        if (CollectionUtil.isNotEmpty(userList)) {
            userId = userList.get(0).getUserId();
        } else {
            User user = userService.registerByEmail(account);
            userId = user.getUserId();
        }

        String token = TokenUtil.generateToken(userId);
        return Result.ok(token);
    }

    @PostMapping("/register")
    Result register(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("code") String code) {
        log.info("用户注册:{}", email);
        
        boolean isEmail = Validator.isEmail(email);
        if (!isEmail) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return Result.error(ResultStatus.PASSWORD_FORMAT_ERROR);
        }

        Object o = redisUtil.get(CacheConstant.REGISTER_CODE + email);
        if (o == null || !o.toString().equalsIgnoreCase(code)) {
            return Result.error(ResultStatus.VERIFY_CODE_ERROR);
        }

        redisUtil.del(CacheConstant.REGISTER_CODE + email);

        List<User> userList = userService.selectUserByEmail(email);
        if (CollectionUtil.isNotEmpty(userList)) {
            return Result.error(ResultStatus.EMAIL_REGISTERED);
        }

        String encryptedPassword = SecureUtil.md5(password);
        User user = userService.registerByEmailAndPassword(email, encryptedPassword);
        String token = TokenUtil.generateToken(user.getUserId());

        return Result.ok(token);
    }

    @PostMapping("/login/password")
    Result loginWithPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        log.info("密码登录:{}", email);
        
        boolean isEmail = Validator.isEmail(email);
        if (!isEmail) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        List<User> userList = userService.selectUserByEmail(email);
        if (CollectionUtil.isEmpty(userList)) {
            return Result.error(ResultStatus.ACCOUNT_NOT_FOUND);
        }

        User user = userList.get(0);
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return Result.error(ResultStatus.PASSWORD_NOT_SET);
        }

        String encryptedPassword = SecureUtil.md5(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            return Result.error(ResultStatus.PASSWORD_ERROR);
        }

        String token = TokenUtil.generateToken(user.getUserId());
        return Result.ok(token);
    }

    @Log
    @Authorization
    @PostMapping("/password/set")
    Result setPassword(
            @UserId String userId,
            @RequestParam("password") String password) {
        log.info("设置密码:{}", userId);
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return Result.error(ResultStatus.PASSWORD_FORMAT_ERROR);
        }

        User user = userService.getUserByUserId(userId);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            return Result.error(ResultStatus.OPERATION_ERROR);
        }

        String encryptedPassword = SecureUtil.md5(password);
        userService.updatePassword(userId, encryptedPassword);

        return Result.ok();
    }

    @Log
    @Authorization
    @PostMapping("/password/update")
    Result updatePassword(
            @UserId String userId,
            @RequestParam("code") String code,
            @RequestParam("newPassword") String newPassword) {
        log.info("修改密码:{}", userId);
        
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            return Result.error(ResultStatus.PASSWORD_FORMAT_ERROR);
        }

        User user = userService.getUserByUserId(userId);
        String email = user.getEmail();
        
        if (email == null || email.isEmpty()) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        Object o = redisUtil.get(CacheConstant.PASSWORD_CODE + email);
        if (o == null || !o.toString().equalsIgnoreCase(code)) {
            return Result.error(ResultStatus.VERIFY_CODE_ERROR);
        }

        redisUtil.del(CacheConstant.PASSWORD_CODE + email);

        String encryptedPassword = SecureUtil.md5(newPassword);
        userService.updatePassword(userId, encryptedPassword);

        return Result.ok();
    }

    @Log
    @Authorization
    @GetMapping("/hasPassword")
    Result<Boolean> hasPassword(@UserId String userId) {
        User user = userService.getUserByUserId(userId);
        boolean hasPassword = user.getPassword() != null && !user.getPassword().isEmpty();
        return Result.ok(hasPassword);
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
    Result deleteUser(
            @UserId String userId,
            @RequestParam("code") String code) {
        log.info("删除账户:{}", userId);
        
        User user = userService.getUserByUserId(userId);
        String email = user.getEmail();
        
        if (email == null || email.isEmpty()) {
            return Result.error(ResultStatus.EMAIL_ERROR);
        }

        Object o = redisUtil.get(CacheConstant.DELETE_ACCOUNT_CODE + email);
        if (o == null || !o.toString().equalsIgnoreCase(code)) {
            return Result.error(ResultStatus.VERIFY_CODE_ERROR);
        }

        redisUtil.del(CacheConstant.DELETE_ACCOUNT_CODE + email);

        userService.deleteUser(userId);
        return Result.ok();
    }
}
