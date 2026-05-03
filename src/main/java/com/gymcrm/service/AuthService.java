package com.gymcrm.service;

import com.gymcrm.domain.Gym;
import com.gymcrm.domain.User;
import com.gymcrm.dto.request.LoginRequest;
import com.gymcrm.dto.response.AuthResponse;
import com.gymcrm.exception.GymInactiveException;
import com.gymcrm.repository.GymRepository;
import com.gymcrm.repository.UserRepository;
import com.gymcrm.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        // SUPER_ADMIN has no gymId — skip gym lookup and status check
        if (user.getGymId() == null) {
            String token = jwtUtil.generateToken(user.getEmail(), null, user.getRole().name());
            return new AuthResponse(token, user.getRole().name(), user.getName(), null);
        }

        Gym gym = gymRepository.findById(user.getGymId())
                .orElseThrow(() -> new IllegalStateException("Gym not found for user"));

        if (gym.getStatus() == Gym.Status.INACTIVE) {
            throw new GymInactiveException("Your gym account is deactivated. Please contact admin.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getGymId(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getName(), gym.getName());
    }
}
