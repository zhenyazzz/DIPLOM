package com.portfolio.core.identity;

import com.portfolio.core.api.identity.UserResponse;
import com.portfolio.core.api.identity.UsersApi;
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
