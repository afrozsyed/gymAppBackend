package com.gymcrm.service;

import com.gymcrm.domain.Gym;
import com.gymcrm.domain.User;
import com.gymcrm.dto.request.CreateGymRequest;
import com.gymcrm.dto.request.ResetPasswordRequest;
import com.gymcrm.dto.response.GymDetailResponse;
import com.gymcrm.exception.DuplicateEmailException;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.repository.GymRepository;
import com.gymcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GymDetailResponse createGym(CreateGymRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        Gym gym = gymRepository.save(Gym.builder()
                .name(request.getGymName())
                .phone(null)
                .build());

        User owner = userRepository.save(User.builder()
                .gymId(gym.getId())
                .name(request.getOwnerName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .build());

        return toDetailResponse(gym, owner);
    }

    public List<GymDetailResponse> getAllGyms() {
        return gymRepository.findAll().stream()
                .map(gym -> {
                    User owner = userRepository.findByGymIdAndRole(gym.getId(), User.Role.ADMIN)
                            .orElse(null);
                    return toDetailResponse(gym, owner);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public GymDetailResponse activateGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found: " + gymId));
        gym.setStatus(Gym.Status.ACTIVE);
        gymRepository.save(gym);
        User owner = userRepository.findByGymIdAndRole(gymId, User.Role.ADMIN).orElse(null);
        return toDetailResponse(gym, owner);
    }

    @Transactional
    public GymDetailResponse deactivateGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found: " + gymId));
        gym.setStatus(Gym.Status.INACTIVE);
        gymRepository.save(gym);
        User owner = userRepository.findByGymIdAndRole(gymId, User.Role.ADMIN).orElse(null);
        return toDetailResponse(gym, owner);
    }

    @Transactional
    public void resetPassword(Long gymId, ResetPasswordRequest request) {
        gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found: " + gymId));
        User owner = userRepository.findByGymIdAndRole(gymId, User.Role.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found for gym: " + gymId));
        owner.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(owner);
    }

    private GymDetailResponse toDetailResponse(Gym gym, User owner) {
        return GymDetailResponse.builder()
                .gymId(gym.getId())
                .gymName(gym.getName())
                .gymPhone(gym.getPhone())
                .status(gym.getStatus().name())
                .ownerName(owner != null ? owner.getName() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerPhone(owner != null ? owner.getPhone() : null)
                .createdAt(gym.getCreatedAt())
                .build();
    }
}
