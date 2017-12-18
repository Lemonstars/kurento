package cn.superid.util;

import java.util.UUID;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2017/12/07
 */
public class UUIDGenerator {

    public static String generatorUUID(){
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        String res = uuidString.replace("-", "");

        return res;
    }

}
