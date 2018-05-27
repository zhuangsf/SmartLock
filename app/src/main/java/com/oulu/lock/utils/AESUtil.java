package com.oulu.lock.utils;

import java.util.Formatter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by liao on 2017/11/7.
 */

public class AESUtil {
    // 加密秘钥 ，16个字节也就是128 bit
    //60 28 13 43 0C 5C 4E 01 0F 21 1E 29 25 66 88 55
    private static final String[] HEX_STRING_KEY = {"60", "28", "13", "43", "0C", "5C", "4E", "01", "0F", "21", "1E", "29", "25", "66", "88", "55"};
    public static final String AES_KEYS = "602813430C5C4E010F211E2925668855";
    //private static final String AES_KEY = "012345678912345611111";
    private static final byte[] AES_KEY = new byte[16];

    // 需要加密的数据(保证16个字节，不够的自己填充)
    private static final String[] SOURCE_STRING_BUF = {"26", "01", "06", "00", "00", "00", "00", "00", "00", "21", "68", "68", "68", "68", "68", "68"};

    private static final byte[] SOURCE_BUF = new byte[16];

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    // Java测试工程入口方法，在这个方法中调用加解密方法并打印结果
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < HEX_STRING_KEY.length; i++) {
            System.out.println(HEX_STRING_KEY[i]);
            System.out.println(hexStringToBytes(HEX_STRING_KEY[i])[0]);
            AES_KEY[i] = hexStringToBytes(HEX_STRING_KEY[i])[0];
        }
        for (int i = 0; i < AES_KEY.length; i++) {
            System.out.println(AES_KEY[i]);
        }

        for (int i = 0; i < SOURCE_STRING_BUF.length; i++) {
            System.out.println(SOURCE_STRING_BUF[i]);
            System.out.println(hexStringToBytes(SOURCE_STRING_BUF[i])[0]);
            SOURCE_BUF[i] = hexStringToBytes(SOURCE_STRING_BUF[i])[0];
        }
        for (int i = 0; i < SOURCE_BUF.length; i++) {
            System.out.println(SOURCE_BUF[i]);
        }

        // 需要加密的原始数据转化成字符串并打印到控制台
        String strSource = BytetohexString(SOURCE_BUF);
        System.out.println("source:\n" + strSource);

        // 调用加密方法，对数据进行加密，加密后的数据存放到encryBuf字节数组中
        byte[] encryBuf = encrypt(AES_KEY, SOURCE_BUF);
        // 将加密后的字节数组数据转成字符串并打印到控制台
        String strEncry = BytetohexString(encryBuf).toLowerCase();
        System.out.println("encrypte:\n" + strEncry);

        // 调用解密方法，对数据进行解密，解密后的数据存放到decryBuf字节数组中
        byte[] decryBuf = decrypt(AES_KEY, encryBuf);
        // 将解密后的字节数组数据转成字符串并打印到控制台
        String strDecry = BytetohexString(decryBuf);
        System.out.println("decrypte:\n" + strDecry);

    }

    // 加密方法
    public static byte[] encrypt(byte[] sKey, byte[] clear) throws Exception {
//		byte[] raw = sKey.getBytes("utf-8");
//		raw=Base64Utils.decode(sKey);
        //	for(int i=0;i<raw.length;i++){
        //	System.out.println("raw:\n" + raw.length);
        //	}

        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    // 解密方法
    public static byte[] decrypt(byte[] sKey, byte[] encrypted)
            throws Exception {
//		byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    // 字节数组按照一定格式转换拼装成字符串用于打印显示
    public static String BytetohexString(byte[] b) {
        int len = b.length;
        StringBuilder sb = new StringBuilder(b.length * (2 + 1));
        Formatter formatter = new Formatter(sb);

        for (int i = 0; i < len; i++) {
            if (i < len - 1)
                formatter.format("%02X", b[i]);
            else
                formatter.format("%02X", b[i]);

        }
        formatter.close();

        return sb.toString();
    }

    public static byte getXOR(byte[] bytes) {
        byte result = 0;
        for (byte b : bytes) {
            result = (byte) (result ^ b);
        }
        return result;
    }
}
