package com.example.EmailClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;
import java.util.List;

@RestController
public class EmailClientServiceController {

    @Autowired
    private EmailClientService emailClientService;

    @GetMapping("/read-emails")
    public List<Email> readEmails() {
        try {
            return emailClientService.readEmails();
        } catch (Exception e) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
        }
    }
}
