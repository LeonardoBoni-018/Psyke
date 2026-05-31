package com.br.psyke.psyke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final Optional<JavaMailSender> mailSender;

    @Value("${app.mail.from:noreply@psyke.com}")
    private String from;

    public void send(String to, String subject, String body) {
        if (mailSender.isEmpty()) {
            log.info("[EMAIL LOGGED] To: {} | Subject: {} | Body: {}", to, subject, body);
            return;
        }
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.get().send(msg);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendConfirmation(String to, String patientName, String professionalName,
                                  String dateTime, String confirmUrl, String cancelUrl) {
        var subject = "Confirmação de Agendamento - Psyke";
        var body = """
            Olá %s,
            
            Seu agendamento com %s foi criado para %s.
            
            Para confirmar: %s
            Para cancelar: %s
            
            Este link é válido por 48 horas.
            
            Atenciosamente,
            Equipe Psyke
            """.formatted(patientName, professionalName, dateTime, confirmUrl, cancelUrl);
        send(to, subject, body);
    }

    public void sendPasswordReset(String to, String name, String resetUrl) {
        var subject = "Recuperação de Senha - Psyke";
        var body = """
            Olá %s,
            
            Recebemos uma solicitação de recuperação de senha.
            
            Para redefinir sua senha, acesse: %s
            
            Este link é válido por 1 hora.
            Se você não solicitou esta alteração, ignore este email.
            
            Atenciosamente,
            Equipe Psyke
            """.formatted(name, resetUrl);
        send(to, subject, body);
    }

    public void sendEmailVerification(String to, String name, String verifyUrl) {
        var subject = "Verificação de Email - Psyke";
        var body = """
            Olá %s,
            
            Bem-vindo à Psyke! Verifique seu email clicando no link abaixo:
            
            %s
            
            Atenciosamente,
            Equipe Psyke
            """.formatted(name, verifyUrl);
        send(to, subject, body);
    }
}
