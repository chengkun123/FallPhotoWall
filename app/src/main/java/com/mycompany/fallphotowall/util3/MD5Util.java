package com.mycompany.fallphotowall.util3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Lenovo on 2017/3/10.
 */
public class MD5Util {
    /*
    * 把url转换为MD5或者hashcode
    * */
    public static String hashKeyFromUrl(String url){
        String  cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());

        } catch (NoSuchAlgorithmException e) {
            cacheKey =String.valueOf(url.hashCode());
        }
        return cacheKey;

    }
    /*
    * 把字节数组转化为十六进制字符串
    * */
    private static String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(int i =0;i<bytes.length;i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
