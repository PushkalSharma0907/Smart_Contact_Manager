package com.smart.smartcontactmanager.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String content;
    
    @Builder.Default
    private MessageType type = MessageType.blue;

	public Message(String content, MessageType type) {
		super();
		this.content = content;
		this.type = type;
	}
    
	// ✅ Getter for content
    public String getContent() {
        return content;
    }

    // ✅ Setter for content (optional)
    public void setContent(String content) {
        this.content = content;
    }

    // ✅ Getter for type
    public MessageType getType() {
        return type;
    }

    // ✅ Setter for type (optional)
    public void setType(MessageType type) {
        this.type = type;
    }

    
}