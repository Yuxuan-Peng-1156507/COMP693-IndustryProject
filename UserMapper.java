package com.nodeam.common.mapper.client;

import com.nodeam.common.domain.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT id, uid, client_id as clientId, name, username, email, password, 2fa as googleSecret, role, status, " +
            "profile_verified as profileVerified, last_login as lastLogin, agent_client_id as agentClientId, " +
            "is_email as isEmail, is_slack as isSlack, add_email as addEmail, pref_lang as prefLang " +
            "FROM users WHERE client_id=#{id}")
    User getByClientId(Long id);

    @Update("UPDATE users SET email = #{email}, social_id = #{socialId}, slack_channel_id = #{slackChannelId}, " +
            "is_email = #{isEmail}, is_slack = #{isSlack}, add_email = #{addEmail}, pref_lang = #{prefLang}, " +
            "updated_at = NOW(), agent_client_id = #{agentUserId} " +
            "WHERE client_id = #{clientId}")
    void updateEmailAndSlackChannel(@Param("clientId") Long clientId,
                                    @Param("email") String email,
                                    @Param("socialId") String socialId,
                                    @Param("slackChannelId") String slackChannelId,
                                    @Param("agentUserId") Long agentUserId,
                                    @Param("isEmail") String isEmail,
                                    @Param("isSlack") String isSlack,
                                    @Param("addEmail") String addEmail,
                                    @Param("prefLang") String prefLang);

    @Insert("INSERT INTO users(uid, client_id, name, email, role, status, " +
            "is_email, is_slack, add_email, pref_lang, " +
            "created_at, updated_at, agent_client_id) " +
            "VALUES(#{uid}, #{clientId}, #{name}, #{email}, #{role}, #{status}, " +
            "#{isEmail}, #{isSlack}, #{addEmail}, #{prefLang}, " +
            "NOW(), NOW(), #{agentClientId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createAgentUser(User user);
}
