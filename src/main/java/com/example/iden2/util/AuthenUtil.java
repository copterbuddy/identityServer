package com.example.iden2.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.example.iden2.model.User;
import com.example.iden2.model.UserToken;
import com.example.iden2.repository.UserRepository;
import com.example.iden2.repository.UserTokenRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuthenUtil {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserTokenRepository userTokenRepository;

    @Autowired
    DateTimeUtil dateTimeUtil;

    public String ValidateTokenServiceStep2(String userToken, String clientId) throws Exception {

        String response = "";

        // UserTokenString Search in UserToken Table
        Optional<UserToken> optToken = userTokenRepository.findByToken(userToken);
        if (optToken == null || optToken.isEmpty()) {
            log.info("kunanonLog ValidateTokenServiceStep2 isNullOrEmpty userToken = {}", userToken);
            response = "404 : not found user token";
            return response;
        }
        UserToken userThisToken = optToken.get();

        // UserToken Search in User
        Optional<User> optUser = userRepository.findById(userThisToken.getRefuserid());
        if (optUser == null || optUser.isEmpty()) {
            log.info("kunanonLog ValidateTokenServiceStep2 isNullOrEmpty optUser = {}", optUser);
            response = "401 = invalid service token";
            return response;
        }

        // Check equals ClientId
        User user = optUser.get();
        if (!user.getClientid().equals(clientId)) {
            log.info("kunanonLog userInfoSerive Service invalid ClientId = {} , userClientId = {}", clientId,
                    user.getClientid());
            response = "401 = invalid service token";
            return response;
        }

        // Check Expired Token
        Date usertime = userThisToken.getExpiredate();
        Date now = Calendar.getInstance().getTime();
        Date expireToken = dateTimeUtil.GetDateInFormatThai(usertime);
        Date timeNow = dateTimeUtil.GetDateInFormatThai(now);

        if (expireToken.before(timeNow)) {
            log.info("kunanonLog ValidateTokenServiceStep2 userToken Expired = {}",
                    dateTimeUtil.GetDateInFormatThaiString(expireToken));
            response = "401 = invalid service token";
            return response;
        }
        return response;
    }

    

    public String CheckRole(String serviceRole, String clientId) throws Exception {
        String response = "";

        List<String> roleList = Arrays.asList(serviceRole);

        Boolean isRole = false;
        for (String role : roleList) {
            isRole = role.toUpperCase().contains(clientId.toUpperCase());
            if (isRole) {
                // role OK
                break;
            }
        }
        if (!isRole) {
            log.info("kunanonLog ValidateTokenServiceStep3 Role invalid");
            response = "401 = invalid service token";
            return response;
        }

        return response;
    }

}
