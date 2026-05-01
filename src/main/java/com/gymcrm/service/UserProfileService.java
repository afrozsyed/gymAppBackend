package com.gymcrm.service;

import com.gymcrm.domain.Gym;
import com.gymcrm.domain.User;
import com.gymcrm.dto.request.ChangePasswordRequest;
import com.gymcrm.dto.request.UpdateProfileRequest;
import com.gymcrm.dto.response.UserProfileResponse;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.repository.GymRepository;
import com.gymcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile() {
        User user = currentUser();
        String gymName = null;
        if (user.getGymId() != null) {
            gymName = gymRepository.findById(user.getGymId())
                    .map(Gym::getName)
                    .orElse(null);
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .gymName(gymName)
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        user.setName(request.getName());
        userRepository.save(user);

        String gymName = null;
        if (user.getRole() == User.Role.ADMIN && user.getGymId() != null
                && request.getGymName() != null && !request.getGymName().isBlank()) {
            Gym gym = gymRepository.findById(user.getGymId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));
            gym.setName(request.getGymName());
            gymRepository.save(gym);
            gymName = gym.getName();
        } else if (user.getGymId() != null) {
            gymName = gymRepository.findById(user.getGymId())
                    .map(Gym::getName)
                    .orElse(null);
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .gymName(gymName)
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        User user = currentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the old password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
