package com.example.wasaas.auth.oauth;

import com.example.wasaas.auth.AuthUserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final GoogleOAuthService googleOAuthService;

    public OAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @PostMapping("/google")
    public AuthUserResponse loginWithGoogle(@Valid @RequestBody GoogleOAuthRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        return googleOAuthService.authenticateWithGoogle(request, httpRequest, httpResponse);
    }
}
