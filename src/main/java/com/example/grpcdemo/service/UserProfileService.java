package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserKind;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfileEntity> findById(UUID userId) {
        return userProfileRepository.findById(userId);
    }

    public UserKind resolveKind(UUID userId) {
        return findById(userId)
                .map(UserProfileEntity::getKind)
                .orElse(UserKind.INDIVIDUAL);
    }
}
