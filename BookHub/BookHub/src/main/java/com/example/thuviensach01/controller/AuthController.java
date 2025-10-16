package com.example.thuviensach01.controller;

import com.example.thuviensach01.entity.Role;
import com.example.thuviensach01.entity.User;
import com.example.thuviensach01.entity.UserActivity;
import com.example.thuviensach01.repository.UserActivityRepository;
import com.example.thuviensach01.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final UserActivityRepository activityRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository userRepo,
                          UserActivityRepository activityRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          SecurityContextRepository securityContextRepository) {
        this.userRepo = userRepo;
        this.activityRepo = activityRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.securityContextRepository = securityContextRepository;
    }

    /* ===== REGISTER ===== */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterReq req, HttpServletRequest request){
        if (userRepo.existsByUsername(req.getUsername()))
            return ResponseEntity.badRequest().body(Map.of("error","Username đã tồn tại"));
        if (userRepo.existsByEmail(req.getEmail()))
            return ResponseEntity.badRequest().body(Map.of("error","Email đã tồn tại"));

        var u = User.builder()
                .username(req.getUsername().trim())
                .email(req.getEmail().trim())
                .password(encoder.encode(req.getPassword()))
                .role(Role.USER)
                .build();
        userRepo.save(u);

        activityRepo.save(UserActivity.of(
                u.getUsername(), "REGISTER", null,
                request.getRemoteAddr(), request.getHeader("User-Agent")
        ));
        return ResponseEntity.ok(Map.of("message","Đăng ký thành công"));
    }

    /* ===== LOGIN ===== */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        // 1) Xác thực
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        // 2) Tạo SecurityContext và lưu tường minh vào HttpSession
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);

        // 3) Ghi log activity
        activityRepo.save(UserActivity.of(
                auth.getName(), "LOGIN", null,
                request.getRemoteAddr(), request.getHeader("User-Agent")
        ));

        var role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_USER");
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", auth.getName(),
                "role", role
        ));
    }

    /*  ME  */
    @GetMapping("/me")
    public ResponseEntity<?> me(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        var role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_USER");
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", auth.getName(),
                "role", role
        ));
    }

    /*  LOGOUT  */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : null;

        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();

        activityRepo.save(UserActivity.of(
                username, "LOGOUT", null,
                request.getRemoteAddr(), request.getHeader("User-Agent")
        ));
        return ResponseEntity.ok(Map.of("message","Đã đăng xuất"));
    }

    /*  DTOs  */
    @Data static class RegisterReq {
        @NotBlank private String username;
        @NotBlank private String email;
        @NotBlank private String password;
    }
    @Data static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
    }
    @Data static class ChangePasswordReq {
        @NotBlank private String oldPassword;
        @NotBlank private String newPassword;
    }
}