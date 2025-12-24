package com.age.b2b.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MailService {
    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender javaMailSender;

    // 인증 코드 저장소
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    // 인증 코드 생성 (6자리)
    public String createCode() {
        Random random = new Random();
        int num = random.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(num);
    }

    // 인증 메일 보내기
    @Async
    public void sendVerifyMail(String email) {
        // 코드 생성 및 저장 로직 추가
        String code = createCode();
        verificationCodes.put(email, code); // 이메일-코드 매핑 저장

        try {
            // HTML 템플릿 로딩
            ClassPathResource resource = new ClassPathResource("templates/email/verifyCode.html");
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 인증코드 삽입
            html = html.replace("{{code}}", code);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name()
            );

            helper.setFrom(sender, "올곧은");
            helper.setTo(email);
            helper.setSubject("[올곧은] 이메일 인증 코드: " + code);
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace(); // 에러 로그 출력
            throw new RuntimeException("인증 메일 전송 실패");
        }
    }

    // 인증 코드 검증 메서드
    public boolean verifyCode(String email, String code) {
        String storedCode = verificationCodes.get(email);
        if (storedCode != null && storedCode.equals(code)) {
            verificationCodes.remove(email); // 인증 성공 시 코드 삭제
            return true;
        }
        return false;
    }

    // 아이디 찾기 메일 발송
    @Async
    public void sendIdMail(String email, String username) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String html = "<div>"
                    + "<h2>[올곧은] 아이디 찾기 안내</h2>"
                    + "<p>회원님의 아이디는 <strong>" + username + "</strong> 입니다.</p>"
                    + "<p>로그인 후 서비스를 이용해주세요.</p>"
                    + "</div>";

            helper.setFrom(sender, "올곧은");
            helper.setTo(email);
            helper.setSubject("[올곧은] 아이디 찾기 안내");
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("메일 발송 실패");
        }
    }

    // 임시 비밀번호 메일 발송
    @Async
    public void sendTempPwMail(String email, String tempPw) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String html = "<div>"
                    + "<h2>[올곧은] 임시 비밀번호 안내</h2>"
                    + "<p>회원님의 임시 비밀번호는 <strong>" + tempPw + "</strong> 입니다.</p>"
                    + "<p>로그인 후 반드시 비밀번호를 변경해주세요.</p>"
                    + "</div>";

            helper.setFrom(sender, "올곧은");
            helper.setTo(email);
            helper.setSubject("[올곧은] 임시 비밀번호 안내");
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("메일 발송 실패");
        }
    }
}