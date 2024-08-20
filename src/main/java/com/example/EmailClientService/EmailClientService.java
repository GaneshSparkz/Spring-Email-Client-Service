package com.example.EmailClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class EmailClientService {

    @Autowired
    private EmailClientServiceConfig emailClientServiceConfig;

    /**
     * Reads unread emails from the inbox folder and parses and returns them.
     * @return emails;
     * @throws MessagingException
     * @throws IOException
     */
    public List<Email> readEmails() throws MessagingException, IOException {
        List<Email> emails = new ArrayList<>();
        Session emailSession = getEmailSession();
        Store store = emailSession.getStore(emailClientServiceConfig.getProtocol());
        store.connect(
                emailClientServiceConfig.getHost(),
                emailClientServiceConfig.getUsername(),
                emailClientServiceConfig.getPassword()
        );
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Message[] messages = inbox.getMessages();

        for (Message message : messages) {
            if (!message.isSet(Flags.Flag.SEEN)) {
                Email email = parseMessage(message);
                emails.add(email);
                message.setFlag(Flags.Flag.SEEN, true);
            }
        }

        return emails;
    }

    private Session getEmailSession() {
        Properties props = new Properties();
        props.put("mail.pop3.host", emailClientServiceConfig.getHost());
        props.put("mail.pop3.port", emailClientServiceConfig.getPort());
        props.put("mail.pop3.auth", "true");
        props.put("mail.pop3.ssl.enable", "true");
        props.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.pop3.socketFactory.fallback", "false");
        props.put("mail.pop3.socketFactory.port", emailClientServiceConfig.getPort());

        return Session.getDefaultInstance(props);
    }

    /**
     * Parses the various parts of an Email Message object and returns the parsed Email
     * @param message
     * @return Email
     * @throws MessagingException
     * @throws IOException
     */
    private Email parseMessage(Message message) throws MessagingException, IOException {
        Email email = new Email();
        email.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());

        email.setTo(getRecipientList(message.getRecipients(Message.RecipientType.TO)));
        email.setCc(getRecipientList(message.getRecipients(Message.RecipientType.CC)));
        email.setBcc(getRecipientList(message.getRecipients(Message.RecipientType.BCC)));

        email.setSubject(message.getSubject());

        if (message.isMimeType("text/*")) {
            email.setBody(message.getContent().toString());
        }
        else if (message.isMimeType("multipart/*")) {
            email.setBody(extractBodyText((MimeMultipart) message.getContent()));
        }

        email.setAttachments(getAttachments(message));

        return email;
    }

    /**
     * Returns the String recipient list from the Address array
     * @param recipients
     * @return recipientList
     */
    private List<String> getRecipientList(Address[] recipients) {
        List<String> recipientList = new ArrayList<>();

        if (recipients == null) return recipientList;

        for (Address recipient : recipients) {
            recipientList.add(recipient.toString());
        }

        return recipientList;
    }

    /**
     * Extracts text from Message body if it is not of plain text type
     * @param message
     * @return body
     * @throws MessagingException
     * @throws IOException
     */
    private String extractBodyText(MimeMultipart message) throws MessagingException, IOException {
        StringBuilder body = new StringBuilder();
        int count = message.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = message.getBodyPart(i);
            if (bodyPart.isMimeType("text/*")) {
                body.append(bodyPart.getContent().toString());
            }
            else if (bodyPart.getContent() instanceof MimeMultipart) {
                body.append(extractBodyText((MimeMultipart) bodyPart.getContent()));
            }
        }

        return body.toString();
    }

    /**
     * Extracts the file names of the attachments of the email.
     * @param message
     * @return attachments
     * @throws MessagingException
     * @throws IOException
     */
    private List<String> getAttachments(Message message) throws MessagingException, IOException {
        List<String> attachments = new ArrayList<>();

        if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();

            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    attachments.add(bodyPart.getFileName());
                }
            }
        }

        return attachments;
    }

}
