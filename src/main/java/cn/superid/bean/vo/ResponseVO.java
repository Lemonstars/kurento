package cn.superid.bean.vo;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public class ResponseVO {

    private Integer code;
    private Object data;

    public ResponseVO(Integer code, Object data) {
        this.code = code;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
