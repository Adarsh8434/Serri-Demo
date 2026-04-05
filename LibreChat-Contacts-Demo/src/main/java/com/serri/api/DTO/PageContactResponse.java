package com.serri.api.DTO;



import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageContactResponse {
    private List<ContactResponseDTO> contacts;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}
