package cn.superid.bean.vo;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public class ChatContentVO {

    private String userId;
    private String chatContent;

    public ChatContentVO(String userId, String chatContent) {
        this.userId = userId;
        this.chatContent = chatContent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatContent() {
        return chatContent;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }

    @Override
    public String toString() {
        return "ChatContentVO{" +
                "userId='" + userId + '\'' +
                ", chatContent='" + chatContent + '\'' +
                '}';
    }
}
