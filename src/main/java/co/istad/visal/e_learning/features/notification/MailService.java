package co.istad.visal.e_learning.features.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends emails in the background. Works only when SMTP is configured (spring.mail.host etc.);
 * otherwise it logs and skips, so the in-app notification still arrives.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String frontendUrl;

    public MailService(ObjectProvider<JavaMailSender> sender,
                       @Value("${app.mail.from:Autumn <no-reply@autumn.local>}") String from,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.sender = sender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    /** True when SMTP is configured, i.e. emails will really go out. */
    public boolean isConfigured() {
        return sender.getIfAvailable() != null;
    }

    /** A branded email with a heading, a short message and one button that opens the app at `link`. */
    @Async
    public void send(String to, String subject, String heading, String message, String buttonLabel, String link) {
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null || to == null || to.isBlank()) {
            log.info("Email not sent (SMTP not configured or no address): {}", subject);
            return;
        }
        try {
            MimeMessage mime = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html(heading, message, buttonLabel, frontendUrl + link), true);
            mail.send(mime);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }

    private static String html(String heading, String message, String buttonLabel, String url) {
        return """
                <div style="background:#f6f6fa;padding:32px 16px;font-family:Arial,Helvetica,sans-serif">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #e3e2ee;border-radius:16px;overflow:hidden">
                    <div style="background:#0d0b61;color:#ffffff;padding:18px 24px;font-size:18px;font-weight:bold">🐰 Autumn · Bunny AI</div>
                    <div style="padding:24px">
                      <h2 style="margin:0 0 12px;color:#14132b;font-size:20px">%s</h2>
                      <p style="margin:0 0 20px;color:#4b4a63;font-size:15px;line-height:1.6;white-space:pre-line">%s</p>
                      <a href="%s" style="display:inline-block;background:#0d0b61;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:10px;font-weight:bold">%s</a>
                    </div>
                  </div>
                  <p style="text-align:center;color:#8a89a3;font-size:12px;margin-top:16px">You received this because you are learning on Autumn.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(heading), HtmlUtils.htmlEscape(message), url, HtmlUtils.htmlEscape(buttonLabel));
    }
}
