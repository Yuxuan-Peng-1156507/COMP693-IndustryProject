package com.nodeam.marketdataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodeam.common.domain.Email;
import com.nodeam.common.mapper.ops.EmailMapper;
import com.nodeam.common.model.EmailAttachment;
import com.nodeam.common.model.PortalConfig;
import com.nodeam.marketdataservice.util.StringUtils;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.files.FilesUploadV2Response;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SlackService {

    private static final Logger logger = LoggerFactory.getLogger(SlackService.class);

    @Value("${nodeam.slack.app_token}")
    public String SLACK_APP_TOKEN;

    @Value("${nodeam.slack.bot_token}")
    public String SLACK_BOT_TOKEN;

    @Autowired
    FileService fileService;

    private MethodsClient getSlackMethod(){
        Slack slack = Slack.getInstance();
        return slack.methods(SLACK_BOT_TOKEN);

    }

    public String sendSlackChannelMsg(String channelName, String message, Long[] fileId) throws SlackApiException, IOException {

        String errorCode = "";

        //Send Message
        if (StringUtils.isNotEmpty(message)) {
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(channelName) // Use a channel ID `C1234567` is preferable
                    .text(message)
                    .build();

            ChatPostMessageResponse response = getSlackMethod().chatPostMessage(request);

            if (!response.isOk()) {
                errorCode = response.getError(); // e.g., "invalid_auth", "channel_not_found"
            }
        }

        //Send Attachment (if any)
        if (StringUtils.isEmpty(errorCode)) {
            if (fileId != null && fileId.length > 0) {
                for (int x = 0; x < fileId.length; x++) {
                    errorCode = sendSlackChannelAttachment(channelName, fileId[x]);

                    if (StringUtils.isNotEmpty(errorCode))
                        System.out.println("!!!! Error sendSlackChannelAttachment " + errorCode);
                }
            }
        }
        return errorCode;
    }

    public String sendSlackChannelAttachmentLocalFilepath(String channelName, String filepath) throws SlackApiException, IOException {
        String errorCode = "";
        File file = new File(filepath);
        if (file != null && StringUtils.isNotEmpty(file.getName())) {

            String filename = file.getName();
            FilesUploadV2Request uploadRequest = FilesUploadV2Request.builder()
                    .file(file)
                    .filename(filename).title(filename)
                    .channel(channelName)
                    .build();

            FilesUploadV2Response uploadResponse = getSlackMethod().filesUploadV2(uploadRequest);
            if (!uploadResponse.isOk()) {
                errorCode = uploadResponse.getError(); // e.g., "invalid_auth", "channel_not_found"
            }
        } else {
            errorCode = "Unable to locate file of filepath: " + filepath;
        }
        return errorCode;
    }

    public String sendSlackChannelAttachment(String channelName, Long fileId) throws SlackApiException, IOException {
        String errorCode = "";

        FileService.FileDto fileDto = fileService.directDownload_GCPBucketFileByFileId(fileId);

        if (fileDto != null
                && fileDto.getBlob() != null
                && fileDto.getBlob().getSize() > 0
                && StringUtils.isNotEmpty(fileDto.getFileName())) {

            FilesUploadV2Request uploadRequest = FilesUploadV2Request.builder()
                    .fileData(fileDto.getBlob().getContent())
                    .filename(fileDto.getFileName())
                    .title(fileDto.getFileName())
                    .channel(channelName)
                    .build();

                FilesUploadV2Response uploadResponse = getSlackMethod().filesUploadV2(uploadRequest);
            if (!uploadResponse.isOk()) {
                errorCode = uploadResponse.getError(); // e.g., "invalid_auth", "channel_not_found"
            }
        } else {
            errorCode = "Unable to locate file of id: " + fileId;
        }
        return errorCode;
    }




    public String sendSlackChannelMsgAttachmentByte(String channelName, String subject, String message, EmailAttachment[] attachmentList) throws SlackApiException, IOException {
        String errorCode = "";


        if (StringUtils.isNotEmpty(channelName)) {

            if (StringUtils.isNotEmpty(message)) {
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(channelName) // Use a channel ID `C1234567` is preferable
                        .text(message)
                        .build();

                ChatPostMessageResponse response = getSlackMethod().chatPostMessage(request);

                if (!response.isOk()) {
                    errorCode += response.getError(); // e.g., "invalid_auth", "channel_not_found"
                    System.out.println("Message Error " + errorCode);
                }
            }

            if (StringUtils.isEmpty(errorCode) && attachmentList != null && attachmentList.length > 0) {
                for (int x = 0;  x< attachmentList.length; x++ ) {

                    FilesUploadV2Request uploadRequest = FilesUploadV2Request.builder()
                            .fileData(attachmentList[x].getContent())
                            .filename(attachmentList[x].getName())
                            .title(attachmentList[x].getName())
                            .channel(channelName)
                            .build();

                    FilesUploadV2Response uploadResponse = getSlackMethod().filesUploadV2(uploadRequest);
                    if (!uploadResponse.isOk()) {
                        errorCode += uploadResponse.getError(); // e.g., "invalid_auth", "channel_not_found"
                        System.out.println("Attachment Error " + errorCode);
                    }
                }
            }

        } else {
            errorCode = "Unable to locate Channel name";
        }


        return errorCode;
    }
}
