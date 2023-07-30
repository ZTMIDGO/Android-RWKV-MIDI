package com.litesnap.open.rwkv.midi;

/**
 * Created by ZTMIDGO 2023/7/21
 */
public class HexUtils {
    public static String charsToHex(char[] chars){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            String hex = Integer.toHexString(chars[i]);
            if (hex.length() % 2 != 0) hex = 0 + hex;
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++){
            String hex = Integer.toHexString(bytes[i] & 0xff);
            if (hex.length() % 2 != 0) hex = "0" + hex;
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex){
        return hexToBytes(hex, -1);
    }

    public static byte[] hexToBytes(String hex, int len){
        if (hex.length() % 2 != 0) hex = "0" + hex;
        int length = hex.length() / 2;
        int start = len > length ? len - length : 0;
        byte[] bytes = new byte[len > length ? len : length];
        for (int i = 0; i < length; i++){
            String item = hex.substring(i * 2, i * 2 + 2);
            bytes[start ++] = (byte) Integer.parseInt(item, 16);
        }
        return bytes;
    }

    public static String intToHex(int val){
        String hex = Integer.toHexString(val);
        if (hex.length() % 2 != 0) hex = "0" + hex;
        return hex;
    }
}
