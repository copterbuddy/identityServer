package com.example.iden2.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.example.iden2.dto.CreateUserRequest;

import org.springframework.stereotype.Component;

@Component
public class GenerateUtil {

    public String generateKey(int keyLen) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keyLen / 8];
        random.nextBytes(bytes);
        return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    }

    public String setStringRole(String item) {
        List<String> wordList = Arrays.asList(item.toString());
        return wordList.toString();
    }

    public String[] getStringRole(String item) {
        String rawString = "";
        String[] response;
        StringBuffer sb = new StringBuffer();
        sb = sb.append(item);// get String
        sb = sb.deleteCharAt(0);// delete first
        sb = sb.deleteCharAt(sb.length() - 1);// delete last
        rawString = sb.toString();
        response = rawString.split(",");

        return response;
    }
}
