package com.example.iden2.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.stereotype.Component;

@Component
public class DateTimeUtil {

    public Date GetDateInFormatThai(Date date) throws Exception {
        String currentFormat = "dd-MM-yyyy HH:mm:ss";
        DateFormat dateFormat = new SimpleDateFormat(currentFormat);
        String strToken = dateFormat.format(date);

        DateFormat srcDF = new SimpleDateFormat(currentFormat);
        srcDF.setTimeZone(TimeZone.getTimeZone("TH"));

        Date parse = srcDF.parse(strToken);

        return parse;
    }

    public Date ConvertDateTimeToThai(String dateString) throws Exception {
        String strGetDateFrom = dateString;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateThai = format.parse(strGetDateFrom);
        dateThai.setHours(dateThai.getHours() + 7);
        return dateThai;
    }

    public String GetDateInFormatThaiString(Date date) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(date);
        return strDate;
    }
}
