package com.example.EmailClientService;

import lombok.Data;

import java.util.List;

@Data
public class Email {
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private List<String> attachments;
}
