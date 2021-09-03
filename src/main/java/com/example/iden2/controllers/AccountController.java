package com.example.iden2.controllers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;

import com.example.iden2.dto.CreateUserRequest;
import com.example.iden2.dto.CreateUserResponse;
import com.example.iden2.dto.DeleteUserRequest;
import com.example.iden2.dto.ListUserRequest;
import com.example.iden2.dto.ListUserResponse;
import com.example.iden2.dto.LoginRequest;
import com.example.iden2.dto.LoginResponse;
import com.example.iden2.dto.RefreshTokenRequest;
import com.example.iden2.dto.RefreshTokenResponse;
import com.example.iden2.dto.SetPasswordRequest;
import com.example.iden2.dto.UpdateUserRequest;
import com.example.iden2.dto.UpdateUserResponse;
import com.example.iden2.dto.UserInfoRequest;
import com.example.iden2.dto.UserInfoResponse;
import com.example.iden2.dto.base.ErrorEntity;
import com.example.iden2.dto.base.ErrorEntityWithLang;
import com.example.iden2.repository.UserRepository;
import com.example.iden2.service.AccountService;
import com.example.iden2.util.ErrorWording;
import com.example.iden2.util.ExceptionUtil;
import com.example.iden2.util.GenerateUtil;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/account")
@Slf4j
public class AccountController {

    @Autowired
    private Validator validator;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ExceptionUtil exceptionUtil;

    @Autowired
    private GenerateUtil getnerateUtil;

    @Autowired
    private ErrorWording er;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest loginRequest) {

        try {
            // validate
            if (!loginRequest.getGrant_type().equals("password") || !loginRequest.getClient_secret().equals("secret")) {
                log.info("kunanonLog Login Controller grantType,secret invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            var violations = validator.validate(loginRequest);
            if (!violations.isEmpty()) {
                var sb = new StringBuilder();
                for (var violation : violations) {
                    sb.append(violation.getPropertyPath() + " " + violation.getMessage());
                    sb.append("n");
                }
                var errorMessage = sb.toString();
                log.info("kunanonLog Login Controller properties invalid : " + errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // business
            LoginResponse response = new LoginResponse();
            response = accountService.login(loginRequest.getUsername(), loginRequest.getPassword(),
                    loginRequest.getClient_id());
            if (response == null) {

                log.info("kunanonLog Login Controller Username Password Notfound");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("kunanonLog account Controller login : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping("/userinfo")
    public ResponseEntity<Object> userInfo(@RequestBody UserInfoRequest userInfoRequest) {

        try {

            UserInfoResponse response = new UserInfoResponse();
            ErrorEntity errorEntity = new ErrorEntity();

            if (StringUtil.isNullOrEmpty(userInfoRequest.getClient_id())) {
                errorEntity.setErrorCode(er._001_INVALID_REQUEST);
            }
            if (StringUtil.isNullOrEmpty(userInfoRequest.getUser_token())
                    || !userInfoRequest.getUser_token().startsWith("Bearer ")) {
                errorEntity.setErrorCode(er._001_INVALID_REQUEST);
            }

            String token = userInfoRequest.getUser_token().substring(7);

            if (StringUtil.isNullOrEmpty(errorEntity.getErrorCode()))
                response = accountService.userInfoSerive(userInfoRequest.getClient_id(), token);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            errorEntity.setErrorCode(response.getErrorCode());
            ErrorEntity createError = exceptionUtil.CreateError(errorEntity);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("kunanonLog account Controller userInfo : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping("/refresh_token")
    public ResponseEntity<Object> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {

        try {
            RefreshTokenResponse response = new RefreshTokenResponse();
            ErrorEntityWithLang errorEntity = new ErrorEntityWithLang();

            if (StringUtil.isNullOrEmpty(refreshTokenRequest.getClient_id())) {
                errorEntity.setErrorCode(er._001_INVALID_REQUEST);
            }
            if (StringUtil.isNullOrEmpty(refreshTokenRequest.getUser_token())
                    || !refreshTokenRequest.getUser_token().startsWith("Bearer ")) {
                errorEntity.setErrorCode(er._001_INVALID_REQUEST);
            }

            String token = refreshTokenRequest.getUser_token().substring(7);

            if (StringUtil.isNullOrEmpty(errorEntity.getErrorCode()))
                response = accountService.refreshToken(refreshTokenRequest.getClient_id(), token);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            errorEntity.setErrorCode(response.getErrorCode());
            ErrorEntityWithLang createError = exceptionUtil.CreateErrorWithLang(errorEntity);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping("/create_user")
    public ResponseEntity<Object> createUser(@RequestBody CreateUserRequest createUserRequest) {

        CreateUserResponse response = new CreateUserResponse();
        ErrorEntityWithLang errorEntity = new ErrorEntityWithLang();

        try {

            var violations = validator.validate(createUserRequest);
            if (!violations.isEmpty()) {
                var sb = new StringBuilder();
                for (var violation : violations) {
                    sb.append(violation.getPropertyPath() + " " + violation.getMessage());
                    sb.append("n");
                }
                var errorMessage = sb.toString();
                return ResponseEntity.badRequest().body(errorMessage);
            }

            response = accountService.createUser(createUserRequest);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            errorEntity.setErrorCode(response.getErrorCode());
            ErrorEntityWithLang createError = exceptionUtil.CreateErrorWithLang(errorEntity);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("kunanonLog account Controller createUser : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/update_user/{user_id}")
    public ResponseEntity<Object> updateUser(@PathVariable("user_id") String userId,
            @RequestBody UpdateUserRequest updateUserRequest) {
        UpdateUserResponse response = new UpdateUserResponse();
        ErrorEntityWithLang errorEntity = new ErrorEntityWithLang();

        try {
            var violations = validator.validate(updateUserRequest);
            if (!violations.isEmpty()) {
                var sb = new StringBuilder();
                for (var violation : violations) {
                    sb.append(violation.getPropertyPath() + " " + violation.getMessage());
                    sb.append("n");
                }
                var errorMessage = sb.toString();
                return ResponseEntity.badRequest().body(errorMessage);
            }

            response = accountService.updateUser(userId, updateUserRequest);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            errorEntity.setErrorCode(response.getErrorCode());
            ErrorEntityWithLang createError = exceptionUtil.CreateErrorWithLang(errorEntity);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info(" kunanonLogaccount Controller updateUser : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/delete_user/{user_id}")
    public ResponseEntity<Object> DeleteUser(@PathVariable("user_id") String userId,
            @RequestBody DeleteUserRequest deleteUserRequest) {

        try {
            ErrorEntityWithLang response = new ErrorEntityWithLang();

            response = accountService.deleteUser(userId, deleteUserRequest);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            ErrorEntityWithLang createError = exceptionUtil.CreateErrorWithLang(response);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("kunanonLog account Controller deleteUser : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping("/set_password/{user_id}")
    public ResponseEntity<Object> setPassword(@PathVariable("user_id") String userId,
            @RequestBody SetPasswordRequest setPasswordRequest) {

        try {
            ErrorEntityWithLang response = new ErrorEntityWithLang();

            response = accountService.setPassword(userId, setPasswordRequest);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            ErrorEntityWithLang createError = exceptionUtil.CreateErrorWithLang(response);

            var mapper = new ModelMapper();
            mapper.map(createError, response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.info("kunanonLog account Controller setPassword : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/list_userinfo")
    public ResponseEntity<Object> listUserInfo(@RequestBody ListUserRequest listUserRequest) {
        ListUserResponse response = new ListUserResponse();
        ErrorEntityWithLang errorEntity = new ErrorEntityWithLang();

        try {

            if (!StringUtil.isNullOrEmpty(listUserRequest.getRegister_date_from())) {
                Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
                Matcher dateFrom = p.matcher(listUserRequest.getRegister_date_from());
                if (!dateFrom.matches()) {
                    response.setErrorCode(er._001_INVALID_REQUEST);
                    log.info("kunanonLog account Controller listUserInfo dateFrom invalid");
                }
            }

            if (!StringUtil.isNullOrEmpty(listUserRequest.getRegister_date_to())) {
                Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
                Matcher dateTo = p.matcher(listUserRequest.getRegister_date_to());
                if (!dateTo.matches()) {
                    response.setErrorCode(er._001_INVALID_REQUEST);
                    log.info("kunanonLog account Controller listUserInfo datetTo invalid");
                }
            }

            if (StringUtil.isNullOrEmpty(response.getErrorCode())) {
                response = accountService.listUserInfo(listUserRequest);
            }

            if (response == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            errorEntity = exceptionUtil.CreateErrorWithLang(response);

            var mapper = new ModelMapper();
            mapper.map(errorEntity, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.info("kunanonLog account Controller listUserInfo : " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
