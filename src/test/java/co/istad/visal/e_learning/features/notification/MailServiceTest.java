package co.istad.visal.e_learning.features.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void embedsTheHeroAndEscapesTheMessage() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(message);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);

        new MailService(provider, "Autumn <a@example.com>", "http://localhost:5173")
                .send("dara@example.com", "Extra help", "You've got extra Bunny AI help",
                        "5 more AI questions <b>now</b>", "Open the lesson", "/courses/1");

        verify(sender).send(message);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        Files.write(Path.of("build/mail-preview.eml"), raw.toByteArray());

        String eml = raw.toString(StandardCharsets.UTF_8);
        assertTrue(eml.contains("Content-ID: <autumn-hero>"));
        assertTrue(eml.contains("image/gif"));
    }
}
