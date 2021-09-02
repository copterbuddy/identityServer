package com.example.iden2.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import com.example.iden2.util.GenerateUtil;

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
    private GenerateUtil generateUtil;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    // @Autowired
    // private EntityManager em;

    public LoginResponse login(String username, String password, String clientId) {

        LoginResponse response = new LoginResponse();

        try {

            // Check existingClient from input
            Optional<Clients> optClients = clientsRepository.findByClientid(clientId);
            if (optClients.isEmpty()) {
                log.info("kunanonLog login Service occur : clientId not existing ");
                return null;
            }
            Clients clients = optClients.get();

            // Validate Username , Password in UserTable
            User user = em
                    .createQuery("select u from User u where u.username = '" + username + "' AND u.password = '"
                            + password + "' AND u.status = 'ACTIVE' AND u.passwordstatus = 'ACTIVE'", User.class)
                    .getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                log.info("kunanonLog login Service occur : user null ");
                return null;
            }

            RequestContextHolder.currentRequestAttributes().setAttribute("username", user.getUsername(),
                    RequestAttributes.SCOPE_REQUEST);

            // Create Token
            String token = generateUtil.generateKey(128);

            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormatter.setLenient(false);
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
        Boolean isProcess = true;

        // TODO: Validate Token Service Flow
        var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes().getAttribute("clientIdApproved",
                RequestAttributes.SCOPE_REQUEST);

        UserInfoResponse response = new UserInfoResponse();

        try {
            // Check Parameter
            if (StringUtil.isNullOrEmpty(userToken)) {
                response.setErrorCode("404");
                isProcess = false;
            }

            if (StringUtil.isNullOrEmpty(clientId)) {
                response.setErrorCode("001");
                isProcess = false;
            }

            if (isProcess) {

                // search token in table user_token
                UserToken existingToken = userTokenRepository.findByToken(userToken).stream().findFirst().orElse(null);
                if (existingToken == null) {
                    return null;
                }

                // check client_id must equals
                if (!clientId.equals(clientIdApproved)) {
                    return null;
                }

                // Check from token
                Optional<User> existingUser = userRepository.findById(existingToken.getRefuserid());
                if (existingUser == null || existingUser.isEmpty()) {
                    return null;
                }
                User user = existingUser.get();
                // Check from input
                // List<User> existingListUser =
                // userRepository.findByClientid(clientsId).stream();
                if (!user.getClientid().equals(clientId)) {
                    return null;
                }

                // check role of userservice must have name of client_id of requester
                List<String> roleList = Arrays.asList(user.getRole());

                Boolean isRole = false;
                for (String role : roleList) {
                    isRole = role.toUpperCase().contains(clientId.toUpperCase());
                    if (isRole) {
                        break;
                    }
                }

                if (isRole) {

                    // userInfo Flow

                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String dateOfBirth = null;
                    String registerDate = null;

                    if (user.getDateofbirth() != null) {
                        dateOfBirth = dateFormat.format(user.getDateofbirth());
                    }

                    if (user.getRegisterdate() != null) {
                        registerDate = dateFormat.format(user.getDateofbirth());
                    }

                    var mapper = new ModelMapper();
                    UserDto userDto = new UserDto();
                    userDto.setUser_id(String.valueOf(user.getId()));
                    userDto.setName(user.getFirstname());
                    userDto.setFamily_name(user.getLastname());
                    userDto.setEmail(user.getEmail());
                    userDto.setClient_id(user.getClientid());
                    userDto.setUser_id(user.getUserid());
                    userDto.setDate_of_birth(dateOfBirth);
                    userDto.setMobile_no(user.getMobileno());
                    userDto.setMobile_country_code(user.getMobilecountrycode());
                    userDto.setStatus(user.getStatus());
                    userDto.setOtp_status(user.getOtpstatus());
                    userDto.setPin_status(user.getPinstatus());
                    userDto.setCitizen_id(user.getCitizenid());
                    userDto.setPassport_no(user.getPassportno());
                    userDto.setId_type(user.getIdtype());
                    userDto.setRole(generateUtil.getStringRole(user.getRole()));
                    userDto.setRegister_date(registerDate);
                    userDto.setCif_no(user.getCifno());

                    UserDto mapped = mapper.map(userDto, UserDto.class);
                    response.setResult(mapped);
                    response.setErrorCode("200");
                } else {
                    return null;
                }

            }

            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog userInfoSerive Service occur : because=" + e);

            return response;
        }

    }

    public RefreshTokenResponse refreshToken(String clientId, String userToken) {

        Boolean isProcess = true;
        RefreshTokenResponse response = new RefreshTokenResponse();

        try {
            // Check Parameter
            if (StringUtil.isNullOrEmpty(userToken)) {
                response.setErrorCode("404");
                isProcess = false;
            }

            if (isProcess) {
                if (StringUtil.isNullOrEmpty(clientId)) {
                    response.setErrorCode("001");
                    isProcess = false;
                }
            }

            if (isProcess) {
                var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                        .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

                // TODO: Validate Service_Token Flow

                // TODO: Validate User_Token Flow

                // TODO: refresh Token Flow

                Optional<UserToken> optUserToken = userTokenRepository.findByToken(userToken);
                if (optUserToken == null || optUserToken.isEmpty()) {
                    return null;
                }

                UserToken getUserToken = optUserToken.get();

                Optional<Clients> optClients = clientsRepository.findByClientid(clientIdApproved);
                Clients clients = optClients.get();

                Date refreshTokenTime = new Date();
                Date endDate = new Date();
                endDate.setMinutes(endDate.getSeconds() + clients.getConsentlifetime());

                getUserToken.setExpiredate(endDate);
                getUserToken.setRefreshdatetime(refreshTokenTime);
                userTokenRepository.save(getUserToken);

                response.setErrorCode("200");
            }

            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog refreshToken Service occur : because=" + e);

            return response;
        }

    }

    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
        log.info("kunanonLog CreateUser Service");

        String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE", LOCK = "LOCK", citizen_id = "citizen_id",
                passport_no = "passport_no";
        CreateUserResponse response = new CreateUserResponse();
        UserDto result = new UserDto();
        Boolean isProcess = true;

        try {

            var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                    .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

            // TODO: Validate Service_Token Flow

            // TODO: Validate User_Token Flow

            // TODO: refresh Token Flow

            User user = new User();

            if (isProcess) {
                user.setClientid(createUserRequest.getClient_id());
            }

            if (isProcess) {
                if (createUserRequest.getStatus().toUpperCase().equals(ACTIVE)
                        || createUserRequest.getStatus().toUpperCase().equals(INACTIVE)
                        || createUserRequest.getStatus().toUpperCase().equals(LOCK)) {
                    user.setStatus(createUserRequest.getStatus());

                } else {
                    response.setErrorCode("001");
                    isProcess = false;
                    log.info("kunanonLog CreateUser Service : getStatus invalid getStatus={}",
                            createUserRequest.getStatus());
                }
            }

            if (isProcess) {
                if (!StringUtil.isNullOrEmpty(createUserRequest.getOtp_status())) {
                    if (!(createUserRequest.getOtp_status().toUpperCase().equals(ACTIVE)
                            || createUserRequest.getOtp_status().toUpperCase().equals(LOCK))) {

                        response.setErrorCode("001");
                        isProcess = false;
                        log.info("kunanonLog CreateUser Service : getStatus invalid getOtp_status={}",
                                createUserRequest.getOtp_status());
                    }
                    if (isProcess) {
                        user.setOtpstatus(createUserRequest.getOtp_status());
                    }
                }
            }

            if (isProcess) {
                if (StringUtil.isNullOrEmpty(createUserRequest.getUser_id())) {
                    createUserRequest.setUser_id(generateUtil.generateKey(128));
                }
            }
            if (isProcess) {
                user.setUserid(createUserRequest.getUser_id());
            }

            if (isProcess) {
                if (StringUtil.isNullOrEmpty(createUserRequest.getId_type())) {
                    user.setIdtype(createUserRequest.getId_type());
                    user.setCitizenid(createUserRequest.getCitizen_id());
                    user.setPassportno(createUserRequest.getPassport_no());
                } else {
                    if (createUserRequest.getId_type().equals(citizen_id)) {
                        user.setIdtype(createUserRequest.getId_type());
                        if (StringUtil.isNullOrEmpty(createUserRequest.getCitizen_id())) {
                            response.setErrorCode("001");
                            isProcess = false;
                            log.info("kunanonLog CreateUser Service : getCitizen_id null_empty");
                        }
                        if (isProcess) {
                            user.setCitizenid(createUserRequest.getCitizen_id());
                            user.setPassportno(createUserRequest.getPassport_no());
                        }

                    } else if (createUserRequest.getId_type().equals(passport_no)) {
                        user.setIdtype(createUserRequest.getId_type());
                        if (StringUtil.isNullOrEmpty(createUserRequest.getPassport_no())) {
                            response.setErrorCode("001");
                            isProcess = false;
                            log.info("kunanonLog CreateUser Service : getPassport_no null_empty");
                        }
                        if (isProcess) {
                            user.setCitizenid(createUserRequest.getCitizen_id());
                            user.setPassportno(createUserRequest.getPassport_no());
                        }
                    } else {
                        response.setErrorCode("001");
                        isProcess = false;
                        log.info("kunanonLog CreateUser Service : getId_type invalid");
                    }
                }
            }

            if (isProcess) {
                if (!StringUtil.isNullOrEmpty(createUserRequest.getDate_of_birth())) {
                    if (isProcess) {
                        try {

                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                            String dateInString = createUserRequest.getDate_of_birth();
                            Date date = formatter.parse(dateInString);
                            user.setDateofbirth(date);

                        } catch (Exception e) {
                            log.info("kunanonLog createUser Service getDate_of_birth ={} : because={}",
                                    createUserRequest.getDate_of_birth(), e);
                            response.setErrorCode("001");
                        }
                    }
                } else {
                    user.setDateofbirth(null);
                }

            }

            if (isProcess) {
                try {
                    if (!StringUtil.isNullOrEmpty(createUserRequest.getRole())) {
                        String sb = generateUtil.setStringRole(createUserRequest.getRole());

                        user.setRole(sb);
                    } else {
                        user.setRole(createUserRequest.getRole().toString());
                    }

                } catch (Exception e) {
                    log.info("kunanonLog createUser Service getRole =" + createUserRequest.getRole() + ": because={}"
                            + e);
                    response.setErrorCode("500");
                }
            }

            if (isProcess) {// Nullable
                user.setEmail(createUserRequest.getEmail());
                user.setUsername(createUserRequest.getUsername());
                user.setPassword(createUserRequest.getPassword());
                user.setFirstname(createUserRequest.getFirstname());
                user.setLastname(createUserRequest.getLastname());
                user.setPin(createUserRequest.getPin());
                user.setMobileno(createUserRequest.getMobile_no());
                user.setMobilecountrycode(createUserRequest.getMobile_country_code());
                user.setPasswordstatus(ACTIVE);
                user.setPinstatus(ACTIVE);
                user.setRegisterdate(new Date());

            }

            if (isProcess) {
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
                result.setRole(generateUtil.getStringRole(userSaved.getRole()));
                result.setRegister_date(registerDate);
                result.setCif_no(userSaved.getCifno());

                response.setResult(result);
                response.setErrorCode("200");
            }

            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
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
            var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                    .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

            // TODO: Validate Service_Token Flow

            // TODO: Validate User_Token Flow

            // TODO: refresh Token Flow

            // update Flow
            User opt = userRepository.findByUserid(userId).stream().findFirst().orElse(null);
            if (opt == null) {
                return null;
            }

            User user = opt;
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
                        response.setErrorCode("001");
                        log.info("kunanonLog createUser Service getCitizen_id null");

                        return response;
                    }

                    user.setIdtype(data.getId_type());
                    user.setCitizenid(data.getCitizen_id());

                    if (StringUtil.isNullOrEmpty(data.getPassport_no())) {
                        user.setPassportno(data.getPassport_no());
                    }

                } else if (data.getId_type().equals(passport_no)) {

                    if (StringUtil.isNullOrEmpty(data.getPassport_no())) {
                        response.setErrorCode("001");
                        log.info("kunanonLog createUser Service getPassport_no null");

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
                    response.setErrorCode("001");
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
            result.setRole(generateUtil.getStringRole(userSaved.getRole()));
            result.setCif_no(userSaved.getCifno());

            response.setResult(result);
            response.setErrorCode("200");

            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog updateUser Service occur : because=" + e);

            return response;
        }

    }

    public ErrorEntityWithLang deleteUser(String userId, DeleteUserRequest deleteUserRequest) {

        ErrorEntityWithLang response = new ErrorEntityWithLang();

        try {

            var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                    .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

            // TODO: Validate Service_Token Flow

            // TODO: Validate User_Token Flow

            // TODO: refresh Token Flow

            // deleteUser Flow
            User opt = userRepository.findByUserid(userId).stream().findFirst().orElse(null);
            if (opt == null) {
                return null;
            }

            User user = opt;
            userRepository.delete(user);

            response.setErrorCode("200");
            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog deleteUser Service occur : because=" + e);

            return response;
        }

    }

    public ErrorEntityWithLang setPassword(String userId, SetPasswordRequest setPasswordRequest) {

        ErrorEntityWithLang response = new ErrorEntityWithLang();

        try {

            var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                    .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

            // TODO: Validate Service_Token Flow

            // TODO: Validate User_Token Flow

            // TODO: refresh Token Flow

            // setPassword Flow
            String status = "ACTIVE";
            User user = em.createQuery(
                    "select u from User u where u.userid = '" + userId + "' AND u.passwordstatus = '" + status + "'",
                    User.class).getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                return null;
            }

            User newUser = new User();
            Date date = new Date();
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            var mapper = new ModelMapper();
            mapper.map(user, newUser);

            newUser.setId(null);

            newUser.setPassword(setPasswordRequest.getPassword());
            newUser.setLastchangepwd(date);

            user.setPasswordstatus("INACTIVE");

            List<User> listUser = new ArrayList<>();
            listUser.add(user);
            listUser.add(newUser);
            userRepository.saveAll(listUser);

            response.setErrorCode("200");
            return response;
        } catch (Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog setPassword Service occur : because=" + e);

            return response;
        }
    }

    public ListUserResponse listUserInfo(ListUserRequest listUserRequest) {

        ListUserResponse response = new ListUserResponse();
        List<UserDto> listDto = new ArrayList<>();
        String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE", LOCK = "LOCK";

        try {

            var clientIdApproved = (String) RequestContextHolder.currentRequestAttributes()
                    .getAttribute("clientIdApproved", RequestAttributes.SCOPE_REQUEST);

            // TODO: Validate Service_Token Flow

            // TODO: Validate User_Token Flow

            // TODO: refresh Token Flow

            // listUserInfo Flow
            var data = listUserRequest;
            String sql = "select u from User u WHERE clientid = '" + data.getClient_id() + "' ";
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
            if (!StringUtil.isNullOrEmpty(data.getRegister_date_from())) {
                sql = sql + "AND u.registerdate >= '" + data.getRegister_date_from() + "' ";
            }
            if (!StringUtil.isNullOrEmpty(data.getRegister_date_to())) {
                sql = sql + "AND u.registerdate <= '" + data.getRegister_date_to() + "' ";
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
                userDto.setUser_id(Long.toString(item.getId()));
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
                userDto.setRole(generateUtil.getStringRole(item.getRole()));
                userDto.setRegister_date(registerDate);
                userDto.setCif_no(item.getCifno());

                listDto.add(mapper.map(userDto, UserDto.class));

            }

            response.setResult(listDto);
            response.setErrorCode("200");
            return response;

        } catch (

        Exception e) {
            response.setErrorCode("500");
            log.info("kunanonLog listUserInfo Service occur : because=" + e);

            return response;
        }
    }
}
