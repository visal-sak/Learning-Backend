package co.istad.visal.e_learning.features.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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

    /**
     * The site's hero animation, recorded: email apps strip scripts and CSS animation but play GIFs.
     * Embedded rather than linked, so it shows without a public server and without "load images".
     * Frame one is the finished hero, for apps that only show the first frame (Outlook on Windows).
     */
    private static final String HERO_ID = "autumn-hero";
    private static final String HERO_FILE = "mail/autumn-hero.gif";

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
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html(heading, message, buttonLabel, frontendUrl + link), true);
            helper.addInline(HERO_ID, new ClassPathResource(HERO_FILE), "image/gif");
            mail.send(mime);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }

    /** The hero's look in email-safe HTML: tables and inline styles, ink outlines, a sticker shadow. */
    private static String html(String heading, String message, String buttonLabel, String url) {
        return """
                <div style="margin:0;background:#f4f2ec;padding:28px 12px;font-family:Arial,Helvetica,sans-serif">
                  <table role="presentation" align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;border-collapse:separate;background:#ffffff;border:3px solid #141414;border-radius:22px;box-shadow:6px 6px 0 #141414">
                    <tr>
                      <td style="padding:0;border-bottom:3px solid #141414;border-radius:19px 19px 0 0;overflow:hidden;background:#ffffff">
                        <img src="cid:%s" width="594" alt="AUTUMN" style="display:block;width:100%%;max-width:594px;height:auto;border:0;border-radius:19px 19px 0 0;font-size:32px;font-weight:bold;color:#141414;text-align:center">
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:26px 28px 30px">
                        <span style="display:inline-block;padding:5px 12px;background:#ffd94a;border:2px solid #141414;border-radius:999px;color:#141414;font-size:13px;font-weight:bold">🐰 Bunny AI</span>
                        <h1 style="margin:16px 0 10px;color:#141414;font-family:'Arial Black',Arial,Helvetica,sans-serif;font-size:24px;line-height:1.25;font-weight:900">%s</h1>
                        <p style="margin:0 0 24px;color:#3d3c4f;font-size:15px;line-height:1.65;white-space:pre-line">%s</p>
                        <a href="%s" style="display:inline-block;padding:13px 24px;background:#0d0b61;border:2px solid #141414;border-radius:999px;box-shadow:3px 3px 0 #141414;color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none">%s &rarr;</a>
                      </td>
                    </tr>
                  </table>
                  <p style="margin:22px 0 0;text-align:center;color:#77766a;font-size:12px;line-height:1.5">You received this because you are learning on Autumn.</p>
                </div>
                """.formatted(HERO_ID, HtmlUtils.htmlEscape(heading), HtmlUtils.htmlEscape(message), HtmlUtils.htmlEscape(url), HtmlUtils.htmlEscape(buttonLabel));
    }
}
