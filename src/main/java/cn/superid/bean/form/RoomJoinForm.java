package cn.superid.bean.form;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public class RoomJoinForm {

    private String sdpOffer;
    private String userId;
    private String roomId;
    private Boolean isPresenter;

    public String getSdpOffer() {
        return sdpOffer;
    }

    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Boolean getIsPresenter() {
        return isPresenter;
    }

    public void setIsPresenter(Boolean isPresenter){
        this.isPresenter = isPresenter;
    }
}
