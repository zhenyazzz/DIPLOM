package com.portfolio.identity.identity;

import com.portfolio.identity.api.identity.UserResponse;
import com.portfolio.identity.api.identity.UsersApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UsersController implements UsersApi {

    private final UserQueryService userQueryService;

    @Override
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userQueryService.me());
    }
}
