package cn.superid.bean.form;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public class ChatContentForm {

    private String userId;
    private String roomId;
    private String chatContent;

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

    public String getChatContent() {
        return chatContent;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }
}
