package com.example.iden2.filter;

import com.example.iden2.model.Clients;
import com.example.iden2.model.User;
import com.example.iden2.model.UserToken;
import com.example.iden2.repository.ClientsRepository;
import com.example.iden2.repository.UserRepository;
import com.example.iden2.repository.UserTokenRepository;
import com.example.iden2.service.AccountService;
import com.example.iden2.service.ClientsService;
import com.example.iden2.util.DateTimeUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientsRepository clientsRepository;

    @Autowired
    UserTokenRepository userTokenRepository;

    @Autowired
    DateTimeUtil dateTimeUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest uri, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {

            // ValidateTokenServiceStep1
            String authorization = uri.getHeader("Authorization");
            String serviceToken = "";

            if (!StringUtil.isNullOrEmpty(authorization) && authorization.startsWith("Bearer ")) {
                serviceToken = authorization.substring(7);
            }

            // Get Service Token
            Optional<UserToken> optUserToken = userTokenRepository.findByToken(serviceToken.toString());
            if (optUserToken == null || optUserToken.isEmpty()) {
                log.info("kunanonLog userToken not found serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            UserToken userToken = optUserToken.get();

            // Check Service Token Expire
            Date serviceTokenExpired = userToken.getExpiredate();
            Date dateTimeNow = new Date();
            Date expiredTime = dateTimeUtil.GetDateInFormatThai(serviceTokenExpired);
            Date nowTime = dateTimeUtil.GetDateInFormatThai(dateTimeNow);
            if (expiredTime.before(nowTime)) {
                log.info("kunanonLog ValidateTokenServiceStep1 serviceToken Expired = {}",
                        dateTimeUtil.GetDateInFormatThaiString(expiredTime));
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }

            Optional<Clients> optClient = clientsRepository.findById(userToken.getRefclientid());
            if (optClient == null || optClient.isEmpty()) {
                log.info("kunanonLog Client not found serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Clients clients = optClient.get();
            if (!clients.getClientid().equals("service")) {
                log.info("kunanonLog serviceToken invalid in Client serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Optional<User> optUser = userRepository.findById(userToken.getRefuserid());
            if (optUser == null || optUser.isEmpty()) {
                log.info("kunanonLog User not found serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            User user = optUser.get();
            if (!user.getClientid().equals("service")) {
                log.info("kunanonLog serviceToken invalid in User serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (StringUtil.isNullOrEmpty(clients.getClientname())
                    || StringUtil.isNullOrEmpty(optUser.get().getRole())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            uri.setAttribute("serviceClientId", optUser.get().getClientid());
            uri.setAttribute("serviceName", clients.getClientname());
            uri.setAttribute("serviceRole", optUser.get().getRole());

            filterChain.doFilter(uri, response);

        } catch (Throwable e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("kunanonLog serviceToken={}" + e);
            return;
        }
    }
}
