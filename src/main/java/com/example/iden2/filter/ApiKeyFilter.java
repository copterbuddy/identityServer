package com.example.iden2.filter;

import com.example.iden2.model.Clients;
import com.example.iden2.model.User;
import com.example.iden2.model.UserToken;
import com.example.iden2.repository.ClientsRepository;
import com.example.iden2.repository.UserRepository;
import com.example.iden2.repository.UserTokenRepository;
import com.example.iden2.service.AccountService;
import com.example.iden2.service.ClientsService;

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

    // @Override
    // protected boolean shouldNotFilter(HttpServletRequest uri) throws
    // ServletException {

    // // if (uri.getRequestURI().startsWith("/api/account/login") ||
    // uri.getRequestURI().startsWith("/api/account/login/"))
    // // return true;

    // // if (uri.getRequestURI().startsWith("/posts") ||
    // // uri.getRequestURI().startsWith("/posts/")) return true;

    // // if (uri.getRequestURI().startsWith("/demo") ||
    // // uri.getRequestURI().startsWith("/demo/")) return true;
    // // // if (uri.getRequestURI().startsWith("/user") ||
    // // uri.getRequestURI().startsWith("/user/")) return true;

    // // return false;

    // }

    @Override
    protected void doFilterInternal(HttpServletRequest uri, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String authorization = uri.getHeader("Authorization");
            String serviceToken = "";

            if (!StringUtil.isNullOrEmpty(authorization) && authorization.startsWith("Bearer ")) {
                serviceToken = authorization.substring(7);
            }

            // try {
            Optional<UserToken> optUserToken = userTokenRepository.findByToken(serviceToken.toString());
            if (optUserToken == null || optUserToken.isEmpty()) {
                log.info("kunanonLog userToken not found serviceToken={}", serviceToken);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UserToken userToken = optUserToken.get();
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

            // uri.setAttribute("clientIdApproved", optUser.get().getClientid());

            filterChain.doFilter(uri, response);

        } catch (Throwable e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("kunanonLog serviceToken={}" + e);
            return;
        }
    }
}
