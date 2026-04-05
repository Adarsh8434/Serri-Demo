package com.serri.api.DTO;


import lombok.Data;

@Data
public class ChatQueryRequest {

    private String message;
    private String contactId;
}
