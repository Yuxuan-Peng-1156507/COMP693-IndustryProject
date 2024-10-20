package com.nodeam.marketdataservice.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodeam.common.domain.Email;
import com.nodeam.common.mapper.ops.EmailMapper;
import com.nodeam.common.model.EmailAttachment;
import com.nodeam.marketdataservice.service.SlackService;
import com.slack.api.methods.SlackApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class MsgSlackTask {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SlackService slackService;

    @Autowired
    private EmailMapper emailMapper;

    @Scheduled(fixedDelay = 5000, initialDelay = 3000)
    public void task() {
        log.info("Starting MsgSlackTask");

        // Handle Slack messages with TO_BE_SENT status
        List<Email> emailsToBeSent = emailMapper.getEmailBySlackDeliveryStatus(Email.TO_BE_SENT);
        for (Email email : emailsToBeSent) {
            processSlackMessage(email, Email.SENT); // Normal message sending
        }

// Handle Slack test messages with TO_BE_TESTED status
       List<Email> emailsToBeTested = emailMapper.getEmailBySlackDeliveryStatus(Email.TO_BE_TESTED);
        for (Email email : emailsToBeTested) {
            // Save the original Slack channel ID
            String originalSlackChannelId = email.getSlackChannelId();

            // Set the test channel ID
            email.setSlackChannelId("XXXXXXXX"); // Test Slack channel

            // Process the test Slack message
            processSlackMessage(email, Email.TESTED);

            // Restore the original Slack channel ID
            email.setSlackChannelId(originalSlackChannelId);

            // Update the restored channel ID to the database
            emailMapper.updateEmail(email); // This should store the restored ID
        }


        log.info("Finished MsgSlackTask");
    }


    private void processSlackMessage(Email email, String finalStatus) {
        try {
            EmailAttachment[] attachments = objectMapper.readValue(email.getAttachments(), EmailAttachment[].class);
            log.info("Attachments size: {}", attachments.length);

            String errorMsg = slackService.sendSlackChannelMsgAttachmentByte(email.getSlackChannelId(), email.getSubject(), email.getSlackMessage(), attachments);
            if (Objects.equals(errorMsg, "")) {
                email.setSlackDeliveryStatus(finalStatus); 
                email.setSlackSentTime(ZonedDateTime.now());
                emailMapper.updateEmail(email);
            } else {
                log.error("Failed to send Slack message: {}", errorMsg);
            }
        } catch (SlackApiException | IOException e) {
            log.error("Error processing Slack message: {}", email.getId(), e);
            throw new RuntimeException(e);
        }
    }
}
