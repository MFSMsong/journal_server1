
package com.uuorb.journal.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class EmailUtil {

    private static final String HOST = "smtp.qq.com";
    private static final String PORT = "465";
    private static final String USERNAME = "1596637465@qq.com";
    private static final String PASSWORD = "ekqumuiijzvfgcfc";
    private static final String FROM = "1596637465@qq.com";
    private static final boolean SSL_ENABLE = true;

    public void sendLoginCode(String email, String code) {
        sendVerifyCode(email, code, "登录");
    }

    public void sendRegisterCode(String email, String code) {
        sendVerifyCode(email, code, "注册");
    }

    public void sendPasswordCode(String email, String code) {
        sendVerifyCode(email, code, "修改密码");
    }

    public void sendDeleteAccountCode(String email, String code) {
        sendVerifyCode(email, code, "注销账户");
    }

    public void sendVerifyCode(String email, String code, String action) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", HOST);
            props.put("mail.smtp.port", PORT);
            props.put("mail.smtp.auth", "true");
            
            if (SSL_ENABLE) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("好享记账 - " + action + "验证码");
            message.setText("您的" + action + "验证码是：" + code + "，5分钟内有效。请勿将验证码告知他人。");

            Transport.send(message);
            log.info("邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("邮件发送失败: {}, email: {}, error: {}", e.getMessage(), email, e);
        }
    }
}

