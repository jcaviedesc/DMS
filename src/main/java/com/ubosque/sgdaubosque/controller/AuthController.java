package com.ubosque.sgdaubosque.controller;

import com.ubosque.sgdaubosque.exception.AppException;
//import com.ubosque.sgdaubosque.exception.AppException;
import com.ubosque.sgdaubosque.model.Profile;
// import com.ubosque.sgdaubosque.model.RoleName;
import com.ubosque.sgdaubosque.model.User;
import com.ubosque.sgdaubosque.payload.ApiResponse;
import com.ubosque.sgdaubosque.payload.JwtAuthenticationResponse;
import com.ubosque.sgdaubosque.payload.LoginRequest;
import com.ubosque.sgdaubosque.payload.SignUpRequest;
import com.ubosque.sgdaubosque.repository.ProfileRepository;
//import com.ubosque.sgdaubosque.repository.RoleRepository;
import com.ubosque.sgdaubosque.repository.UserRepository;
import com.ubosque.sgdaubosque.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;

/**
 * Created by rajeevkumarsingh on 02/08/17.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider tokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        // if(userRepository.existsByUsername(signUpRequest.getUsername())) {
        //     return new ResponseEntity(new ApiResponse(false, "Username is already taken!"),
        //             HttpStatus.BAD_REQUEST);
        // }

        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity(new ApiResponse(false, "Email Address already in use!"),
                    HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        User user = new User(signUpRequest.getName(),signUpRequest.getLastName(),
                signUpRequest.getEmail(), signUpRequest.getPassword());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        Profile useProfile = profileRepository.findById(signUpRequest.getProfileId())
                .orElseThrow(() -> new AppException("User Role not set."));

        user.setProfiles(Collections.singleton(useProfile));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(result.getName()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
    }
}