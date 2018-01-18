package cn.superid.util;

import cn.superid.bean.vo.ResponseVO;
import cn.superid.constant.ErrorCode;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public class ResponseUtil {

    public static ResponseVO successResponse(Object data){
        return new ResponseVO(0, data);
    }

    public static ResponseVO errorResponse(ErrorCode errorCode){
        return new ResponseVO(errorCode.getCode(), errorCode.getMessage());
    }

}
