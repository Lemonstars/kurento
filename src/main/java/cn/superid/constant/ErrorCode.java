package cn.superid.constant;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public enum  ErrorCode {

    ROOM_NOT_EXIST(1000, "The room don't exist"),
    USER_ON_VIDEO(2000, "The user is busy");

    private Integer code;
    private String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
