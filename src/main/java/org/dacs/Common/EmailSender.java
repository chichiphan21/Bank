package org.dacs.Common;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    public static void sendEmail(String to, String subject, String content) {
        // Thông tin cấu hình SMTP server
//        App name: thong-pro
//        App password : qfxq rusa xsym sjpj
//        User name: chichiphan217@gmail.com

        final String username = "chichiphan217@gmail.com"; // Email của bạn
        final String password = "qfxq rusa xsym sjpj"; // Mật khẩu ứng dụng

        String host = "smtp.gmail.com"; // Gmail SMTP server
        int port = 587; // Port của Gmail SMTP

        // Thiết lập thuộc tính
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        // Xác thực tài khoản
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Tạo email
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            // Gửi email
            Transport.send(message);
            System.out.println("Email đã được gửi thành công!");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Gửi email thất bại!");
        }
    }
//
//    public static void main(String[] args) {
//        sendEmail("recipient@example.com", "Test Email", "Đây là nội dung email.");
//    }
}
