package com.nodeam.marketdataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodeam.common.domain.Email;
import com.nodeam.common.mapper.ops.EmailMapper;
import com.nodeam.common.model.PortalConfig;
import com.nodeam.marketdataservice.util.StringUtils;
import com.wildbit.java.postmark.Postmark;
import com.wildbit.java.postmark.client.ApiClient;
import com.wildbit.java.postmark.client.data.model.message.Message;
import com.wildbit.java.postmark.client.data.model.message.MessageResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Value("${app.mail.sender}")
    public String sender;

    @Value("${app.mail.token}")
    public String token;

    @Value("${app.sitename}")
    public String siteName;

    @Value("${app.frontend}")
    public String siteUrl;

    @Value("${nodeam.isTestEnv}")
    private boolean isTestEnv;

    @Value("${nodeam.isSendMail}")
    private boolean isSendMail;

    @Value("${app.mail.techreceiver}")
    public String techReceiver;

    @Value("${app.mail.salesReceiver}")
    public String salesReceiver;

    @Value("${app.mail.opsReceiver}")
    public String opsReceiver;

    private ApiClient client = null;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final EmailMapper emailMapper; // Inject EmailMapper via constructor
    @Autowired
    public MailService(EmailMapper emailMapper) {
        this.emailMapper = emailMapper;
    }

    @Autowired
    private ResourceLoader resourceLoader;

    public static class EmailTemplate {
        public String subject;
        public String body;
    }

    public static class SlackTemplate {
        public String body;
    }

    public static class EmailAttachment {
        public String name;
        public byte[] content;
        public String contentType;

        public EmailAttachment(String name, byte[] content, String contentType) {
            this.name = name;
            this.content = content;
            this.contentType = contentType;
        }
    }

    private Map<String, EmailTemplate> templates = new HashMap<>();
    private Map<String, SlackTemplate> slackTemplates = new HashMap<>();

    public ApiClient getClient(){
        if(client == null){
            System.out.println("token = " + token);
            client = Postmark.getApiClient(token);
        }
        return client;
    }


//    @PostConstruct
//    public void initialization() {
//        try {
//            System.out.println("initializing email and Slack services...");
//            // load subjects
//            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("subject.json");
//            HashMap<String, String> subjects = new HashMap<>();
//            if (inputStream != null) {
//                subjects = new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, String>>() {});
//            }
//            System.out.println("subjects: " + subjects);
//
//            // load contents
//            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
//            Resource[] resources = resolver.getResources("classpath*:/email/*.html");
//
//            loadTemplates(resources, subjects);
//        } catch (Exception e) {
//            System.err.println("Error initializing: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void loadTemplates(Resource[] resources, HashMap<String, String> subjects) throws Exception {
//        if (resources == null) {
//            System.err.println("No templates found.");
//            return;
//        }
//        for (Resource resource : resources) {
//            String filename = resource.getFilename();
//            String key = filename.substring(0, filename.lastIndexOf('.'));
//            if (key.endsWith("-slack")) { // check Slack template
//                SlackTemplate slackTemplate = new SlackTemplate();
//                slackTemplate.body = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
//                slackTemplates.put(key, slackTemplate);
//                System.out.println("Added Slack template: " + key);
//            } else {
//                EmailTemplate template = new EmailTemplate();
//                template.body = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
//                if (subjects.containsKey(key)) {
//                    template.subject = subjects.get(key);
//                } else {
//                    template.subject = "NODEAM OPS CRON JOB ALERT";
//                }
//                templates.put(key, template);
//                System.out.println("Added email template: " + key + " subject: " + template.subject);
//            }
//        }
//    }
//
//
//
//    public MessageResponse send(String to, String subject, String body) throws Exception {
//        Message message = new Message(sender, to, subject, body);
//        MessageResponse response = getClient().deliverMessage(message);
//        return response;
//    }
//    public MessageResponse internalSend(boolean isClient, String to, String cc, String bcc, String template, Map<String, String> substitutes, String strMessageStream) throws Exception {
//        return internalSend(isClient, to, cc, bcc, template, substitutes, strMessageStream, null);
//    }
//
//
//    public MessageResponse internalSend(boolean isClient, String to, String cc, String bcc, String template, Map<String, String> substitutes, String strMessageStream, String slackChannelId) throws Exception {
//
//        if (!isSendMail) {
//            System.out.println("TEST To Send Email to " + to + " :: bcc " + bcc);
//            System.out.println(substitutes.toString());
//            return null;
//        }
//
//        if(isTestEnv) {
//            to = "Email " + to + " <" + techReceiver + ">";
//            if (StringUtils.isNotEmpty(cc)) {
//                cc = "Test Env " + cc + "<" + techReceiver + ">";
//            }
//            if (StringUtils.isNotEmpty(bcc)) {
//                bcc = "Test Env " + bcc + "<" + techReceiver + ">";
//            }
//        }
//
//        if(!templates.containsKey(template)){
//            return null;
//        }
//        if(substitutes == null){
//            substitutes = new HashMap<>();
//        }
//        if(!substitutes.containsKey("SITE_NAME")){
//            substitutes.put("SITE_NAME", siteName);
//        }
//        if(!substitutes.containsKey("SITE_URL")){
//            substitutes.put("SITE_URL", siteUrl);
//        }
//
//        EmailTemplate tpl = templates.get(template);
//        String subject = tpl.subject;
//        String body = tpl.body;
//        for(String key: substitutes.keySet()){
//            subject = subject.replace("{{" + key + "}}", substitutes.get(key));
//            body = body.replace("{{" + key + "}}", substitutes.get(key));
//        }
//
//        Message message = new Message(sender, to, subject, body);
//        if (isClient) {
//            // 将 salesreceiver 和 operationsEmail 添加到 BCC
//            bcc += ";" + salesReceiver + ";" + opsReceiver;
//        }
//
//        if (StringUtils.isNotEmpty(cc)) {
//            message.setCc(cc);
//        }
//        if (StringUtils.isNotEmpty(bcc)) {
//            message.setBcc(bcc);
//        }
//
//        if (StringUtils.isNotEmpty(strMessageStream)) {
//            message.setMessageStream(strMessageStream);
//        }
//
//        Email email = messageToEmail(message, template, new ObjectMapper().writeValueAsString(substitutes));
//        PortalConfig config = (PortalConfig) redisTemplate.opsForValue().get("portal-config");
//
//        // handle Slack template
//        SlackTemplate slackTemplate = slackTemplates.get(template + "-slack");
//        if (slackTemplate != null) {
//            String slackMessage = slackTemplate.body;
//            for (String key : substitutes.keySet()) {
//                String value = substitutes.get(key);
//                if (value == null) {
//                    value = "";
//                }
//                slackMessage = slackMessage.replace("{{" + key + "}}", value);
//            }
//            email.setSlackMessage(slackMessage);
//
//            if (slackChannelId != null) {
//                email.setSlackChannelId(slackChannelId);
//            } else {
//                String emptySlackChannelIdPassed = "warn_empty_slackChannelId_passed";
//                logger.warn("Empty slackChannelId is passed, set to {}", emptySlackChannelIdPassed);
//                email.setSlackChannelId(emptySlackChannelIdPassed);
//            }
//        }else{
//            logger.info("there is no Slack template, so Slack related message will not be populated into DB");
//        }
//
//        if (config != null && config.getAutoEmail()) {
//            MessageResponse response = getClient().deliverMessage(message);
//            email.setDeliveryStatus(Email.SENT);
//            email.setEmailSentTime(ZonedDateTime.now());
//            if (slackTemplate != null) {
//                email.setSlackDeliveryStatus(Email.TO_BE_SENT);
//            }
//            emailMapper.createEmail(email);
//            return response;
//        } else {
//            email.setDeliveryStatus(Email.TO_BE_REVIEWED);
//            if (slackTemplate != null) {
//                email.setSlackDeliveryStatus(Email.TO_BE_REVIEWED);
//            }
//            emailMapper.createEmail(email);
//            return null;
//        }
//    }
//


    //mainly used for client communication, email template is required!!
    public MessageResponse sendBasedOnClientSetting(String to, String cc, String bcc, String slackChannelId, String template, Map<String, String> substitutes, List<EmailAttachment> attachmentList, boolean eventEmailOnHold, boolean clientEmailOnHold, boolean clientSlackOnHold) throws Exception {
        // BCC appears for mClient related
        if (StringUtils.isNotEmpty(bcc) && isTestEnv) {
            to = "Test Email " + to + " <" + techReceiver + ">";
            cc = "Test Email " + cc + " <" + techReceiver + ">";
            bcc = "Test Email " + bcc + " <" + techReceiver + ">";
        }

        if (!templates.containsKey(template)) {
            return null;
        }

        if (substitutes == null) {
            substitutes = new HashMap<>();
        }

        if (!substitutes.containsKey("SITE_NAME")) {
            substitutes.put("SITE_NAME", siteName);
        }
        if (!substitutes.containsKey("SITE_URL")) {
            substitutes.put("SITE_URL", siteUrl);
        }

        EmailTemplate emailTemplate = templates.getOrDefault(template, null);
        SlackTemplate slackTemplate = slackTemplates.getOrDefault(template + "-slack", null);

        if (emailTemplate != null) { //note: email template shall be in place, otherwise slack msg won't be sent!!
            String subject = emailTemplate.subject;
            String body = emailTemplate.body;
            for (String key : substitutes.keySet()) {
                String value = substitutes.get(key);
                if (value == null) {
                    value = "";
                }
                subject = subject.replace("{{" + key + "}}", value);
                body = body.replace("{{" + key + "}}", value);
            }

            Message message = new Message(sender, to, subject, body);
            boolean isIgnoreSendConfig = false;
            if (StringUtils.equalsIgnoreCase("login-notice", template)) {
                isIgnoreSendConfig = true;
            } else {
                if (!StringUtils.equalsIgnoreCase(to, techReceiver)) {
                    if (StringUtils.isEmpty(bcc)) {
                        bcc = salesReceiver + ";" + opsReceiver;
                    } else{
                        bcc += ";" + salesReceiver + ";" + opsReceiver;
                    }
                }
                if (bcc !=null)
                    message.setBcc(bcc);

                if (StringUtils.isNotEmpty(cc) )
                    message.setCc(cc);
            }

            if (attachmentList != null && attachmentList.size() > 0) {
                for (EmailAttachment attachment : attachmentList) {
                    message.addAttachment(attachment.name, attachment.content, attachment.contentType);
                }
            }

            Email email = messageToEmail(message, template, new ObjectMapper().writeValueAsString(substitutes));
            PortalConfig config = (PortalConfig) redisTemplate.opsForValue().get("portal-config");
            if (slackTemplate != null) {
                String slackMessage = slackTemplate.body;
                for (String key : substitutes.keySet()) {
                    String value = substitutes.get(key);
                    if (value == null) {
                        value = "";
                    }
                    slackMessage = slackMessage.replace("{{" + key + "}}", value);
                }
                email.setSlackMessage(slackMessage);

                if (slackChannelId != null) {
                    email.setSlackChannelId(slackChannelId);
                } else {
                    String emptySlackChannelIdPassed = "warn_empty_slackChannelId_passed";
                    logger.warn("Empty slackChannelId is passed, set to {}",  emptySlackChannelIdPassed);
                    email.setSlackChannelId(emptySlackChannelIdPassed);
                }
            }else{
                logger.info("there is no Slack template, so Slack related message will not be populated into DB");
            }

            if ( (isIgnoreSendConfig)  || (config != null && config.getAutoEmail() && !eventEmailOnHold)) {
                String sendStatus = "";
                if (clientEmailOnHold){
                    email.setDeliveryStatus(Email.NO_EMAIL_FOR_THIS_CLIENT);
                } else {
                    getClient().deliverMessage(message);
                    email.setDeliveryStatus(Email.SENT);
                    email.setEmailSentTime(ZonedDateTime.now());
                    logger.info("need to send email");
                    sendStatus = "email is sent; ";
                }
                if (clientSlackOnHold) {
                    email.setSlackDeliveryStatus(Email.NO_SLACK_FOR_THIS_CLIENT);
                } else {
                    if (slackTemplate != null) {
                        email.setSlackDeliveryStatus(Email.TO_BE_SENT);
                        logger.info("need to send slack");
                        sendStatus += "slack is sent";
                    }
                }
                emailMapper.createEmail(email);
                MessageResponse sendResponse = new MessageResponse();
                sendResponse.setMessage(sendStatus);
                return sendResponse;
            } else {
                email.setDeliveryStatus(Email.TO_BE_REVIEWED);
                if (slackTemplate != null){
                    email.setSlackDeliveryStatus(Email.TO_BE_REVIEWED);
                }
            }
            emailMapper.createEmail(email);
        } else {
            logger.info("there is no email template, no email will be sent");
        }
        return null; //end of sendBasedOnClientSetting
    }



//    private String processEmailAddress (String address){
//        String processedAddress ="";
//        for (String addr:address.split(";")) {
//            if(addr.contains("@")){
//                processedAddress+=addr + ";";
//            }
//        }
//        return processedAddress;
//    }
//    public Message emailToMessage(Email email) throws JsonProcessingException {
//        Message message = new Message();
//        message.setFrom(email.getSender());
//        message.setTo(email.getRecipient());
//        message.setCc(email.getCc());
//        message.setBcc(email.getBcc() == null? "": processEmailAddress(email.getBcc()));
//        message.setReplyTo(email.getReplyTo());
//        message.setSubject(email.getSubject());
//        message.setHtmlBody(email.getHtmlBody());
//        message.setTextBody(email.getTextBody());
//        message.setTag(email.getTag());
//        message.setMessageStream(email.getMessageStream());
//        message.setTrackOpens(email.getTrackOpens());
//        message.setAttachments(new ObjectMapper().readValue(email.getAttachments(), new TypeReference<List<Map<String,String>>>(){}));
//        return message;
//    }
//
//
//    private Email messageToEmail(Message message, String template, String substitutes) throws JsonProcessingException {
//        Email email = new Email();
//        email.setSender(message.getFrom());
//        email.setRecipient(message.getTo());
//        email.setCc(message.getCc());
//        email.setBcc(message.getBcc());
//        email.setReplyTo(message.getReplyTo());
//        email.setSubject(message.getSubject());
//        email.setHtmlBody(message.getHtmlBody());
//        email.setTextBody(message.getTextBody());
//        email.setTag(message.getTag());
//        email.setMessageStream(message.getMessageStream());
//        email.setTrackOpens(message.getTrackOpens());
//        email.setTrackLinks(message.getTrackLinks());
//        email.setAttachments(convertAttachments(message.getAttachments()));
//
//        email.setTemplate(template);
//        email.setSubstitutes(substitutes);
//
//        return email;
//    }
//
//    private String convertAttachments(List<Map<String, String>> attachments) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            return objectMapper.writeValueAsString(attachments);
//        } catch (JsonProcessingException e) {
//            logger.error("Error converting attachments to JSON string: " + e.getMessage());
//        }
//        return null;
//    }
//}
