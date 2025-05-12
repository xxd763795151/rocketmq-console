package org.apache.rocketmq.console.common;

import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @Author xuxd
 * @Date 4:18 下午 2020/11/18
 * @Description SendMailClient
 **/
@Component
public class SendMailClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendMailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public boolean sendWithAttachment(String from, String to, String subject, String content,
        Map<String, String> name2paths) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            for (Map.Entry<String, String> entry : name2paths.entrySet()) {
                helper.addAttachment(entry.getKey(), new FileSystemResource(entry.getValue()));
            }
            mailSender.send(mimeMessage);
            LOGGER.info(String.format("email send success： from=%s, to=%s, subject=%s, content=%s", from, to, subject, content));
            return true;
        } catch (Exception e) {
            LOGGER.error(String.format("email send fail： from=%s, to=%s, subject=%s, content=%s", from, to, subject, content), e);
            return false;
        }
    }

    public boolean send(String from, String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
            LOGGER.info(String.format("email send succes： from=%s, to=%s, subject=%s, content=%s", from, to, subject, content));
            return true;
        } catch (Exception e) {
            LOGGER.error(String.format("email send fail： from=%s, to=%s, subject=%s, content=%s", from, to, subject, content), e);
            return false;
        }
    }

    public boolean sendHtml(String from, String to, String subject, String template, Map<String, Object> vars) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            context.setVariables(vars);
            String content = templateEngine.process(template, context);

            helper.setText(content, true);

            mailSender.send(mimeMessage);
            LOGGER.info(String.format("email send success： from=%s, to=%s, subject=%s", from, to, subject));
            return true;
        } catch (Exception e) {
            LOGGER.error(String.format("email send fail： from=%s, to=%s, subject=%s", from, to, subject), e);
            return false;
        }
    }

    public boolean sendHtml(String from, String to, String subject, String template, Map<String, Object> vars, StringBuilder errorMsg) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            context.setVariables(vars);
            String content = templateEngine.process(template, context);

            helper.setText(content, true);

            mailSender.send(mimeMessage);
            LOGGER.info(String.format("email send success： from=%s, to=%s, subject=%s", from, to, subject));
            return true;
        } catch (Exception e) {
            LOGGER.error(String.format("email send fail： from=%s, to=%s, subject=%s", from, to, subject), e);
            errorMsg.append(e.getMessage());
            return false;
        }
    }
}
