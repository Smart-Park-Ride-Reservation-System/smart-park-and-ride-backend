package com.project.smartparking.dto;

import com.project.smartparking.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;

    public static UserSummaryDto from(User user) {
        if (user == null) return null;
        return new UserSummaryDto(user.getUserId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}