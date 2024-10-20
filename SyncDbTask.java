package com.nodeam.marketdataservice.task;

import com.nodeam.common.domain.PmsClient;
import com.nodeam.common.domain.User;
import com.nodeam.common.mapper.client.UserMapper;
import com.nodeam.common.mapper.ops.PmsClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncDbTask extends BaseFetchTask {
    @Autowired
    private PmsClientMapper pmsClientMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * Sync from db_pms.PmsClient to db_client.User every 1 minute (60 seconds)
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 5000)
    public void syncPmsClientToUser() {
        log.info("Starting sync task from ops.PmsClient to client.User for upsert");
        try {
            List<PmsClient> pmsClients = pmsClientMapper.fetchClientRecordWithEmailSlack(new PmsClient());
            log.debug("Fetched {} PmsClients from the database: {}", pmsClients.size(), pmsClients);

            for (PmsClient pmsClient : pmsClients) {
                log.debug("Processing PmsClient: {}", pmsClient);

                User user = userMapper.getByClientId(pmsClient.getClientId());
                if (user != null) {
                    log.debug("Found existing User with clientId: {}. Updating fields with values - Email: {}, SlackChannel: {}," +
                                    " AgentUserId: {}, IsEmail: {}, IsSlack: {}, AddEmail: {}, PrefLang: {}",
                            pmsClient.getClientId(), pmsClient.getEmail(), pmsClient.getSlackChannel(), pmsClient.getAgentUserId(),
                            pmsClient.getIsEmail(), pmsClient.getIsSlack(), pmsClient.getAddEmail(), pmsClient.getPrefLang());

                    userMapper.updateEmailAndSlackChannel(
                            pmsClient.getClientId(),
                            pmsClient.getEmail(),
                            pmsClient.getAccountNumber(), // account number to be used as social id, e.g. IN242199
                            pmsClient.getSlackChannel(),
                            pmsClient.getAgentUserId(),
                            pmsClient.getIsEmail(),
                            pmsClient.getIsSlack(),
                            pmsClient.getAddEmail(),
                            pmsClient.getPrefLang()


                    );

                    log.info("Updated User with clientId: {}", pmsClient.getClientId());
                } else {
                    log.debug("No User found with clientId: {}. Creating new User with values - Email: {}, " +
                                    "SlackChannel: {}, AgentUserId: {}, IsEmail: {}, IsSlack: {}, AddEmail: {}, PrefLang: {}",
                            pmsClient.getClientId(), pmsClient.getEmail(), pmsClient.getSlackChannel(),
                            pmsClient.getAgentUserId(), pmsClient.getIsEmail(), pmsClient.getIsSlack(), pmsClient.getAddEmail(), pmsClient.getPrefLang());

                    User manageUser = new User();
                    manageUser.setUid(pmsClient.getClientId().toString());
                    manageUser.setClientId(pmsClient.getClientId());
                    manageUser.setEmail(pmsClient.getEmail());
                    manageUser.setName(pmsClient.getClientName());
                    manageUser.setRole("mclient");
                    manageUser.setStatus(pmsClient.getClientStatus());
                    manageUser.setProfileVerified(false);
                    manageUser.setAgentClientId(pmsClient.getAgentUserId());
                    manageUser.setIsEmail(pmsClient.getIsEmail());
                    manageUser.setIsSlack(pmsClient.getIsSlack());
                    manageUser.setAddEmail(pmsClient.getAddEmail());
                    manageUser.setPrefLang(pmsClient.getPrefLang());


                    userMapper.createAgentUser(manageUser);
                    log.info("Created new User with clientId: {}", pmsClient.getClientId());
                }
            }

            log.info("Sync task completed successfully");
        } catch (Exception e) {
            log.error("An error occurred during the sync task", e);
        }
    }
}
