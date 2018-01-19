package cn.superid.constant;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public enum  ErrorCode {

    ROOM_NOT_EXIST(1000, "The room does't exist"),
    USER_ON_VIDEO(2000, "The user is busy"),
    USER_NOT_ON_VIDEO(2001, "The user does't join the video");

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
