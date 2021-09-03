package com.example.iden2.service;

import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import com.example.iden2.dto.CreateUserRequest;
import com.example.iden2.dto.CreateUserResponse;
import com.example.iden2.dto.DeleteUserRequest;
import com.example.iden2.dto.ListUserRequest;
import com.example.iden2.dto.ListUserResponse;
import com.example.iden2.dto.LoginResponse;
import com.example.iden2.dto.RefreshTokenResponse;
import com.example.iden2.dto.SetPasswordRequest;
import com.example.iden2.dto.UpdateUserRequest;
import com.example.iden2.dto.UpdateUserResponse;
import com.example.iden2.dto.UserDto;
import com.example.iden2.dto.UserInfoResponse;
import com.example.iden2.dto.base.ErrorEntityWithLang;
import com.example.iden2.model.Clients;
import com.example.iden2.model.User;
import com.example.iden2.model.UserToken;
import com.example.iden2.repository.ClientsRepository;
import com.example.iden2.repository.UserRepository;
import com.example.iden2.repository.UserTokenRepository;
import com.example.iden2.util.AuthenUtil;
import com.example.iden2.util.CustomWording;
import com.example.iden2.util.DateTimeUtil;
import com.example.iden2.util.ErrorWording;
import com.example.iden2.util.GenerateUtil;

import org.apache.logging.log4j.util.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Transactional
@Slf4j
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private GenerateUtil getnerateUtil;

    @Autowired
    DateTimeUtil dateTimeUtil;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    AuthenUtil authenUtil;

    @Autowired
    CustomWording cw;

    @Autowired
    ErrorWording er;

    // @Autowired
    // private EntityManager em;

    public LoginResponse login(String username, String password, String clientId) {

        LoginResponse response = new LoginResponse();

        try {
            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // CheckRole
            String checkRole = authenUtil.CheckRole(serviceRole, clientId);
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                log.info("kunanonLog login Service invalid role");
                return null;
            }

            // Check existingClient from input
            Optional<Clients> optClients = clientsRepository.findByClientid(clientId);
            if (optClients.isEmpty()) {
                log.info("kunanonLog login Service occur : clientId not existing ");
                return null;
            }
            Clients clients = optClients.get();

            // Validate Username , Password in UserTable
            User user = em.createQuery(
                    "select u from User u where u.username = '" + username + "' AND u.password = '" + password
                            + "' AND u.status = '" + cw.ACTIVE + "' AND u.passwordstatus = '" + cw.ACTIVE + "' ",
                    User.class).getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                log.info("kunanonLog login Service occur : user null ");
                return null;
            }

            if (!user.getClientid().equals(clientId)) {
                log.info("kunanonLog login Service occur : service is not match");
                return null;
            }

            // RequestContextHolder.currentRequestAttributes().setAttribute("clientName",
            // user.getUsername(),
            // RequestAttributes.SCOPE_REQUEST);

            // Create Token
            String token = getnerateUtil.generateKey(128);

            Date startDate = new Date();
            Date endDate = new Date();
            endDate.setMinutes(endDate.getSeconds() + clients.getConsentlifetime());

            // Save Token to UserTonkenTable
            UserToken userToken = new UserToken();
            userToken.setToken(token);
            userToken.setStartdate(startDate);
            userToken.setExpiredate(endDate);
            userToken.setStatus("Active");
            userToken.setRefuserid(user.getId());
            userToken.setRefclientid(clients.getId());
            userTokenRepository.save(userToken);

            // Return Token
            response.setAccess_token(token);
            response.setExpires_in(clients.getConsentlifetime());
            response.setToken_type("Bearer");

            return response;

        } catch (Exception e) {
            log.info("kunanonLog login Service occur : because=" + e);
            // handle exception
            return null;
        }

    }

    public UserInfoResponse userInfoSerive(String clientId, String userToken) {

        UserInfoResponse response = new UserInfoResponse();

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // Check Parameter
            if (StringUtil.isNullOrEmpty(userToken)) {
                log.info("kunanonLog userInfoSerive Service isNullOrEmpty userToken = {}", userToken);
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            if (StringUtil.isNullOrEmpty(clientId)) {
                log.info("kunanonLog userInfoSerive Service isNullOrEmpty clientId = {}", clientId);
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            // Validate Token Service Step 2
            String authen_2 = authenUtil.ValidateTokenServiceStep2(userToken, clientId);
            if (!StringUtil.isNullOrEmpty(authen_2)) {
                return response;
            }

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, clientId);
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }

            // UserInfo Flow
            Optional<UserToken> optToken = userTokenRepository.findByToken(userToken);
            if (optToken == null || optToken.isEmpty()) {
                log.info("kunanonLog userInfoSerive Service isNullOrEmpty userToken = {}", userToken);
                response.setErrorCode(er._401_INVALID_SERVICE_TOKEN);
                return response;
            }
            UserToken userThisToken = optToken.get();

            Optional<User> optUser = userRepository.findById(userThisToken.getRefuserid());
            if (optUser == null || optUser.isEmpty()) {
                log.info("kunanonLog userInfoSerive Service isNullOrEmpty optUser = {}", optUser);
                response.setErrorCode(er._401_INVALID_SERVICE_TOKEN);
                return response;
            }

            // Check equals ClientId
            User user = optUser.get();
            if (!user.getClientid().equals(clientId)) {
                log.info("kunanonLog userInfoSerive Service invalid ClientId = {} , userClientId = {}", clientId,
                        user.getClientid());
                response.setErrorCode(er._401_INVALID_SERVICE_TOKEN);
                return response;
            }

            var mapper = new ModelMapper();
            UserDto userDto = new UserDto();
            userDto.setId(user.getId().toString());
            userDto.setName(user.getFirstname());
            userDto.setFamily_name(user.getLastname());
            userDto.setEmail(user.getEmail());
            userDto.setClient_id(user.getClientid());
            userDto.setUser_id(user.getUserid());
            userDto.setDate_of_birth(dateTimeUtil.GetDateInFormatThaiString(user.getDateofbirth()));
            userDto.setMobile_no(user.getMobileno());
            userDto.setMobile_country_code(user.getMobilecountrycode());
            userDto.setStatus(user.getStatus());
            userDto.setOtp_status(user.getOtpstatus());
            userDto.setPin_status(user.getPinstatus());
            userDto.setCitizen_id(user.getCitizenid());
            userDto.setPassport_no(user.getPassportno());
            userDto.setId_type(user.getIdtype());
            userDto.setRole(getnerateUtil.getStringRole(user.getRole()));
            userDto.setRegister_date(dateTimeUtil.GetDateInFormatThaiString(user.getRegisterdate()));
            userDto.setCif_no(user.getCifno());

            UserDto mapped = mapper.map(userDto, UserDto.class);
            response.setResult(mapped);
            response.setErrorCode(er._200_SUCCESS);

            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog userInfoSerive Service occur : because=" + e);

            return response;
        }

    }

    public RefreshTokenResponse refreshToken(String clientId, String userToken) {

        RefreshTokenResponse response = new RefreshTokenResponse();

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // Check Parameter
            if (StringUtil.isNullOrEmpty(userToken)) {
                log.info("kunanonLog refreshToken Service userToken IsNullEmpty");
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            if (StringUtil.isNullOrEmpty(clientId)) {
                log.info("kunanonLog refreshToken Service clientId IsNullEmpty");
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            // Validate Token Service Step 2
            String authen_2 = authenUtil.ValidateTokenServiceStep2(userToken, clientId);
            if (!StringUtil.isNullOrEmpty(authen_2)) {
                return response;
            }

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, clientId);
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }

            // refresh Token Flow
            Optional<UserToken> optUserToken = userTokenRepository.findByToken(userToken);
            if (optUserToken == null || optUserToken.isEmpty()) {
                log.info("kunanonLog refreshToken Service userToken IsNullEmpty");
                response.setErrorCode(er._401_INVALID_SERVICE_TOKEN);
                return response;
            }

            UserToken getUserToken = optUserToken.get();

            Optional<Clients> optClients = clientsRepository.findByClientid(clientId);
            Clients clients = optClients.get();

            Date refreshTokenTime = new Date();
            Date endDate = new Date();
            endDate.setMinutes(endDate.getSeconds() + clients.getConsentlifetime());

            getUserToken.setExpiredate(endDate);
            getUserToken.setRefreshdatetime(refreshTokenTime);
            userTokenRepository.save(getUserToken);

            response.setErrorCode(er._200_SUCCESS);

            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog refreshToken Service occur : because=" + e);

            return response;
        }

    }

    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
        CreateUserResponse response = new CreateUserResponse();
        UserDto result = new UserDto();

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            User user = new User();

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, createUserRequest.getClient_id());
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }

            // Search in Client Table
            Optional<Clients> clientIdApprove = clientsRepository.findByClientid(createUserRequest.getClient_id());

            if (clientIdApprove == null || clientIdApprove.isEmpty()) {
                log.info("kunanonLog CreateUser Service : clientId Not found clientId={}",
                        createUserRequest.getClient_id());
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }
            user.setClientid(createUserRequest.getClient_id());

            if (createUserRequest.getStatus().toUpperCase().equals(cw.ACTIVE)
                    || createUserRequest.getStatus().toUpperCase().equals(cw.INACTIVE)
                    || createUserRequest.getStatus().toUpperCase().equals(cw.LOCK)) {
                user.setStatus(createUserRequest.getStatus());

            } else {
                response.setErrorCode(er._001_INVALID_REQUEST);
                log.info("kunanonLog CreateUser Service : getStatus invalid getStatus={}",
                        createUserRequest.getStatus());
                return response;
            }

            if (!StringUtil.isNullOrEmpty(createUserRequest.getOtp_status())) {
                if (!createUserRequest.getOtp_status().toUpperCase().equals(cw.ACTIVE)
                        && createUserRequest.getOtp_status().toUpperCase().equals(cw.LOCK)) {

                    response.setErrorCode(er._001_INVALID_REQUEST);
                    log.info("kunanonLog CreateUser Service : getStatus invalid getOtp_status={}",
                            createUserRequest.getOtp_status());
                    return response;
                }

                user.setOtpstatus(createUserRequest.getOtp_status());
            }

            if (StringUtil.isNullOrEmpty(createUserRequest.getUser_id())) {
                createUserRequest.setUser_id(getnerateUtil.generateKey(128));
            }
            user.setUserid(createUserRequest.getUser_id());

            if (StringUtil.isNullOrEmpty(createUserRequest.getId_type())) {
                user.setIdtype(createUserRequest.getId_type());
                user.setCitizenid(createUserRequest.getCitizen_id());
                user.setPassportno(createUserRequest.getPassport_no());
            } else {
                if (createUserRequest.getId_type().equals(cw.citizen_id)) {
                    user.setIdtype(createUserRequest.getId_type());
                    if (StringUtil.isNullOrEmpty(createUserRequest.getCitizen_id())) {
                        response.setErrorCode(er._001_INVALID_REQUEST);
                        log.info("kunanonLog CreateUser Service : getCitizen_id null_empty");
                        return response;
                    }
                    user.setCitizenid(createUserRequest.getCitizen_id());
                    user.setPassportno(createUserRequest.getPassport_no());

                } else if (createUserRequest.getId_type().equals(cw.passport_no)) {
                    user.setIdtype(createUserRequest.getId_type());
                    if (StringUtil.isNullOrEmpty(createUserRequest.getPassport_no())) {
                        response.setErrorCode(er._001_INVALID_REQUEST);
                        log.info("kunanonLog CreateUser Service : getPassport_no null_empty");
                        return response;
                    }
                    user.setCitizenid(createUserRequest.getCitizen_id());
                    user.setPassportno(createUserRequest.getPassport_no());
                } else {
                    response.setErrorCode(er._001_INVALID_REQUEST);
                    log.info("kunanonLog CreateUser Service : getId_type invalid");
                    return response;
                }
            }

            if (!StringUtil.isNullOrEmpty(createUserRequest.getDate_of_birth())) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                    String dateInString = createUserRequest.getDate_of_birth();
                    Date date = formatter.parse(dateInString);
                    user.setDateofbirth(date);

                } catch (Exception e) {
                    log.info("kunanonLog createUser Service getDate_of_birth ={} : because={}",
                            createUserRequest.getDate_of_birth(), e);
                    response.setErrorCode(er._001_INVALID_REQUEST);
                    return response;
                }
            } else {
                user.setDateofbirth(null);
            }

            try {
                if (!StringUtil.isNullOrEmpty(createUserRequest.getRole())) {
                    String sb = getnerateUtil.setStringRole(createUserRequest.getRole());

                    user.setRole(sb);
                } else {
                    user.setRole(createUserRequest.getRole().toString());
                }

            } catch (Exception e) {
                log.info("kunanonLog createUser Service getRole =" + createUserRequest.getRole() + ": because={}" + e);
                throw e;
            }

            user.setEmail(createUserRequest.getEmail());
            user.setUsername(createUserRequest.getUsername());
            user.setPassword(createUserRequest.getPassword());
            user.setFirstname(createUserRequest.getFirstname());
            user.setLastname(createUserRequest.getLastname());
            user.setPin(createUserRequest.getPin());
            user.setMobileno(createUserRequest.getMobile_no());
            user.setMobilecountrycode(createUserRequest.getMobile_country_code());
            user.setPasswordstatus(cw.ACTIVE);
            user.setPinstatus(cw.ACTIVE);
            user.setRegisterdate(new Date());

            User userSaved = userRepository.save(user);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateOfBirth = null;
            String registerDate = null;

            if (userSaved.getDateofbirth() != null) {
                dateOfBirth = dateFormat.format(userSaved.getDateofbirth());
            }
            if (userSaved.getRegisterdate() != null) {
                registerDate = dateFormat.format(userSaved.getRegisterdate());
            }

            result.setId(String.valueOf(userSaved.getId()));
            result.setName(userSaved.getFirstname());
            result.setFamily_name(userSaved.getLastname());
            result.setEmail(userSaved.getEmail());
            result.setClient_id(userSaved.getClientid());
            result.setUser_id(userSaved.getUserid());
            result.setDate_of_birth(dateOfBirth);
            result.setMobile_no(userSaved.getMobileno());
            result.setMobile_country_code(userSaved.getMobilecountrycode());
            result.setStatus((userSaved.getStatus()));
            result.setOtp_status(userSaved.getOtpstatus());
            result.setPin_status(userSaved.getPinstatus());
            result.setCitizen_id(userSaved.getCitizenid());
            result.setPassport_no(userSaved.getPassportno());
            result.setId_type(userSaved.getIdtype());
            result.setRole(getnerateUtil.getStringRole(userSaved.getRole()));
            result.setRegister_date(registerDate);
            result.setCif_no(userSaved.getCifno());

            response.setResult(result);
            response.setErrorCode(er._200_SUCCESS);

            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog createUser Service occur : because=" + e);

            return response;
        }

    }

    public UpdateUserResponse updateUser(String userId, UpdateUserRequest updateUserRequest) {
        UpdateUserResponse response = new UpdateUserResponse();
        UserDto result = new UserDto();
        var data = updateUserRequest;
        String citizen_id = "citizen_id", passport_no = "passport_no";

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, updateUserRequest.getClient_id());
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                log.info("kunanonLog updateUser Service checkRole invalid");
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            // update Flow
            User user = em
                    .createQuery("select u from User u where u.userid = '" + userId + "' AND u.status = '" + cw.ACTIVE
                            + "' AND u.passwordstatus = '" + cw.ACTIVE + "' ", User.class)
                    .getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                log.info("kunanonLog updateUser Service user not found");
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            if (!StringUtil.isNullOrEmpty(data.getFirstname())) {
                user.setFirstname(data.getFirstname());
            }
            if (!StringUtil.isNullOrEmpty(data.getLastname())) {
                user.setLastname(data.getLastname());
            }
            if (!StringUtil.isNullOrEmpty(data.getEmail())) {
                user.setEmail(data.getEmail());
            }
            if (!StringUtil.isNullOrEmpty(data.getUsername())) {
                user.setUsername(data.getUsername());
            }
            if (!StringUtil.isNullOrEmpty(data.getMobile_no())) {
                // user.setMobileno(data.getMobile_no));
            }
            if (!StringUtil.isNullOrEmpty(data.getMobile_country_code())) {
                user.setMobilecountrycode(data.getMobile_country_code());
            }
            if (!StringUtil.isNullOrEmpty(data.getId_type())) {// TODO:
                if (data.getId_type().equals(citizen_id)) {

                    if (StringUtil.isNullOrEmpty(data.getCitizen_id())) {
                        response.setErrorCode(er._001_INVALID_REQUEST);
                        log.info("kunanonLog updateUser Service getCitizen_id null");

                        return response;
                    }

                    user.setIdtype(data.getId_type());
                    user.setCitizenid(data.getCitizen_id());

                    if (StringUtil.isNullOrEmpty(data.getPassport_no())) {
                        user.setPassportno(data.getPassport_no());
                    }

                } else if (data.getId_type().equals(passport_no)) {

                    if (StringUtil.isNullOrEmpty(data.getPassport_no())) {
                        response.setErrorCode(er._001_INVALID_REQUEST);
                        log.info("kunanonLog updateUser Service getPassport_no null");

                        return response;

                    }

                    user.setIdtype(data.getId_type());
                    user.setCitizenid(data.getCitizen_id());

                    if (!StringUtil.isNullOrEmpty(data.getCitizen_id())) {
                        user.setCitizenid(data.getCitizen_id());
                    }

                    user.setIdtype(data.getId_type());

                } else {
                    log.info("kunanonLog updateUser Service error occur getId_type={}", data.getId_type());
                    response.setErrorCode(er._001_INVALID_REQUEST);
                }
            } else {
                if (!StringUtil.isNullOrEmpty(data.getCitizen_id())) {
                    user.setCitizenid(data.getCitizen_id());
                }
                if (!StringUtil.isNullOrEmpty(data.getPassport_no())) {
                    user.setPassportno(data.getPassport_no());
                }
            }

            if (!StringUtil.isNullOrEmpty(data.getDate_of_birth())) {// TODO:
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                String dateInString = data.getDate_of_birth();
                Date date = formatter.parse(dateInString);
                user.setDateofbirth(date);
            }

            User userSaved = userRepository.save(user);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateOfBirth = null;

            if (userSaved.getDateofbirth() != null) {
                dateOfBirth = dateFormat.format(userSaved.getDateofbirth());
            }

            result.setId(String.valueOf(userSaved.getId()));
            result.setName(userSaved.getLastname());
            result.setFamily_name(userSaved.getLastname());
            result.setEmail(userSaved.getEmail());
            result.setClient_id(userSaved.getClientid());
            result.setUser_id(userSaved.getUserid());
            result.setDate_of_birth(dateOfBirth);
            result.setMobile_no(userSaved.getMobileno());
            result.setMobile_country_code(userSaved.getMobilecountrycode());
            result.setStatus((userSaved.getStatus()));
            result.setOtp_status(userSaved.getOtpstatus());
            result.setPin_status(userSaved.getPinstatus());
            result.setCitizen_id(userSaved.getCitizenid());
            result.setPassport_no(userSaved.getPassportno());
            result.setId_type(userSaved.getIdtype());
            result.setRole(getnerateUtil.getStringRole(userSaved.getRole()));
            result.setCif_no(userSaved.getCifno());

            response.setResult(result);
            response.setErrorCode(er._200_SUCCESS);

            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog updateUser Service occur : because=" + e);

            return response;
        }

    }

    public ErrorEntityWithLang deleteUser(String userId, DeleteUserRequest deleteUserRequest) {

        ErrorEntityWithLang response = new ErrorEntityWithLang();

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, deleteUserRequest.getClient_id());
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }

            // deleteUser Flow
            User user = em
                    .createQuery("select u from User u where u.userid = '" + userId + "' AND u.passwordstatus = '"
                            + cw.ACTIVE + "' AND u.status = '" + cw.ACTIVE + "' ", User.class)
                    .getResultList().stream().findFirst().orElse(null);
            // User user = opt;
            user.setStatus(cw.DELETE);
            userRepository.save(user);

            response.setErrorCode(er._200_SUCCESS);
            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog deleteUser Service occur : because=" + e);

            return response;
        }

    }

    public ErrorEntityWithLang setPassword(String userId, SetPasswordRequest setPasswordRequest) {

        ErrorEntityWithLang response = new ErrorEntityWithLang();

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, setPasswordRequest.getClient_id());
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }

            // setPassword Flow
            User user = em
                    .createQuery("select u from User u where u.userid = '" + userId + "' AND u.passwordstatus = '"
                            + cw.ACTIVE + "' AND u.status = '" + cw.ACTIVE + "' ", User.class)
                    .getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                log.info("kunanonLog deleteUser Service user not found ");
                response.setErrorCode(er._001_INVALID_REQUEST);
                return response;
            }

            User newUser = new User();
            Date date = new Date();
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            var mapper = new ModelMapper();
            mapper.map(user, newUser);

            newUser.setId(null);

            newUser.setPassword(setPasswordRequest.getPassword());
            newUser.setLastchangepwd(date);

            user.setPasswordstatus(cw.INACTIVE);
            user.setStatus(cw.INACTIVE);

            List<User> listUser = new ArrayList<>();
            listUser.add(user);
            listUser.add(newUser);
            userRepository.saveAll(listUser);

            response.setErrorCode(er._200_SUCCESS);
            return response;
        } catch (Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog setPassword Service occur : because=" + e);

            return response;
        }
    }

    public ListUserResponse listUserInfo(ListUserRequest listUserRequest) {

        ListUserResponse response = new ListUserResponse();
        List<UserDto> listDto = new ArrayList<>();
        String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE", LOCK = "LOCK";

        try {

            String serviceRole = (String) RequestContextHolder.currentRequestAttributes().getAttribute("serviceRole",
                    RequestAttributes.SCOPE_REQUEST);

            // checkRole
            String checkRole = authenUtil.CheckRole(serviceRole, listUserRequest.getClient_id());
            if (!StringUtil.isNullOrEmpty(checkRole)) {
                response.setErrorCode(checkRole);
                return response;
            }
            String strDateFrom = "";
            String strDateTo = "";
            if (listUserRequest.getRegister_date_from() != null) {

                Date dateThaiFrom = dateTimeUtil.ConvertDateTimeToThai(listUserRequest.getRegister_date_from());
                strDateFrom = dateTimeUtil.GetDateInFormatThaiString(dateThaiFrom);
            }
            if (listUserRequest.getRegister_date_to() != null) {
                Date dateThaiTo = dateTimeUtil.ConvertDateTimeToThai(listUserRequest.getRegister_date_to());
                strDateTo = dateTimeUtil.GetDateInFormatThaiString(dateThaiTo);
            }

            // listUserInfo Flow
            var data = listUserRequest;
            if (data.getInclude_delete() == null) {
                data.setInclude_delete(false);
            }

            String sql = "select u from User u WHERE u.clientid = '" + data.getClient_id() + "' ";
            if (!StringUtil.isNullOrEmpty(data.getUsername())) {
                sql = sql + "AND u.username = '" + data.getUsername() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getUser_id())) {
                sql = sql + "AND u.userid = '" + data.getUser_id() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getFirstname())) {
                sql = sql + "AND u.firstname LIKE '%" + data.getFirstname() + "%' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getLastname())) {
                sql = sql + "AND u.lastname LIKE '%" + data.getLastname() + "%' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getCif_no())) {
                sql = sql + "AND u.cifno = '" + data.getCif_no() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getId_no())) {
                sql = sql + "AND (u.citizenid = '" + data.getId_no() + "' OR u.passportno = '" + data.getCif_no()
                        + "') ";
            }
            if (!StringUtil.isNullOrEmpty(data.getMobile_no())) {
                sql = sql + "AND u.mobileno = '" + data.getMobile_no() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getEmail())) {
                sql = sql + "AND u.email = '" + data.getEmail() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(strDateFrom)) {
                sql = sql + "AND u.registerdate >= '" + strDateFrom + "' ";
            }
            if (!StringUtil.isNullOrEmpty(strDateTo)) {
                sql = sql + "AND u.registerdate <= '" + strDateTo + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getStatus())) {
                if (data.getStatus().equals(ACTIVE) || data.getStatus().equals(INACTIVE)
                        || data.getStatus().equals(LOCK)) {
                    sql = sql + "AND u.Status = '" + data.getStatus() + "' ";
                }
            }
            if (!StringUtil.isNullOrEmpty(data.getOtp_status())) {
                if (data.getOtp_status().equals(ACTIVE) || data.getOtp_status().equals(LOCK)) {
                    sql = sql + "AND u.Status = '" + data.getOtp_status() + "' ";
                }
            }
            if (!StringUtil.isNullOrEmpty(data.getOtp_status())) {
                if (data.getOtp_status().equals(ACTIVE) || data.getOtp_status().equals(LOCK)) {
                    sql = sql + "AND u.otpstatus = '" + data.getOtp_status() + "' ";
                }
            }
            if (!StringUtil.isNullOrEmpty(data.getPin_status())) {
                if (data.getPin_status().equals(ACTIVE) || data.getPin_status().equals(LOCK)) {
                    sql = sql + "AND u.pinstatus = '" + data.getPin_status() + "' ";
                }
            }
            if (!data.getInclude_delete()) {
                sql = sql + "AND u.status != '" + cw.DELETE + "' ";
            }

            Query query = em.createQuery(sql);
            int pageNumber = data.getPage() - 1;
            if (pageNumber < 0) {
                pageNumber = Integer.MAX_VALUE;
            }
            int pageSize = data.getRow_per_page();
            if (pageSize <= 0) {
                pageSize = 100;
            }
            query.setFirstResult((pageNumber) * pageSize);
            query.setMaxResults(pageSize);
            List<User> listUser = query.getResultList();

            // userInfo Flow

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateOfBirth = null;
            String registerDate = null;

            var mapper = new ModelMapper();

            for (User item : listUser) {
                if (item.getDateofbirth() != null) {
                    dateOfBirth = dateFormat.format(item.getDateofbirth());
                }

                if (item.getRegisterdate() != null) {
                    registerDate = dateFormat.format(item.getDateofbirth());
                }

                UserDto userDto = new UserDto();
                userDto.setId(Long.toString(item.getId()));
                userDto.setName(item.getFirstname());
                userDto.setFamily_name(item.getLastname());
                userDto.setEmail(item.getEmail());
                userDto.setClient_id(item.getClientid());
                userDto.setUser_id(item.getUserid());
                userDto.setDate_of_birth(dateOfBirth);
                userDto.setMobile_no(item.getMobileno());
                userDto.setMobile_country_code(item.getMobilecountrycode());
                userDto.setStatus(item.getStatus());
                userDto.setOtp_status(item.getOtpstatus());
                userDto.setPin_status(item.getPinstatus());
                userDto.setCitizen_id(item.getCitizenid());
                userDto.setPassport_no(item.getPassportno());
                userDto.setId_type(item.getIdtype());
                userDto.setRole(getnerateUtil.getStringRole(item.getRole()));
                userDto.setRegister_date(registerDate);
                userDto.setCif_no(item.getCifno());

                listDto.add(mapper.map(userDto, UserDto.class));

            }

            response.setResult(listDto);
            response.setErrorCode(er._200_SUCCESS);
            return response;

        } catch (

        Exception e) {
            response.setErrorCode(er._500_INTERNAL_SERVER_ERROR);
            log.info("kunanonLog listUserInfo Service occur : because=" + e);

            return response;
        }
    }
}
