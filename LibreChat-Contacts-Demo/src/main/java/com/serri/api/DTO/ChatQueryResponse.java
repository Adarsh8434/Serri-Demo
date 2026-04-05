package com.serri.api.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ChatQueryResponse {
    private String response;
    private List<String> contactIdsUsed;
    private int contactsInjected;
}
