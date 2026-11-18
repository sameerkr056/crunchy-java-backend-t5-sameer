package com.crunchycode.cabbooking.dto.response;

import com.crunchycode.cabbooking.model.AppUser;
import com.crunchycode.cabbooking.model.Role;

/** Safe view of a user: the password hash is never included. */
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;

    public UserResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
