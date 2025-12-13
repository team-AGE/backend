package com.age.b2b.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MailServiceTest {
    @Autowired MailService mailService;

    @Test
    public void mailSendTest() throws Exception {
        // given
        String email = "ug034516@gmail.com";

        // when
        mailService.sendVerifyMail(email, "123456");

        // then

    }
}