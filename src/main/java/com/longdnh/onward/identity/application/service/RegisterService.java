package com.longdnh.donot_get_left_behind.identity.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.longdnh.donot_get_left_behind.identity.application.RegistrationPolicy;
import com.longdnh.donot_get_left_behind.identity.application.exception.EmailAlreadyExistsException;
import com.longdnh.donot_get_left_behind.common.id.IdGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.PasswordHasher;
import com.longdnh.donot_get_left_behind.identity.application.port.UserRepository;
import com.longdnh.donot_get_left_behind.identity.domain.User;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final PasswordHasher passwordHasher;
    private final RegistrationPolicy registrationPolicy;
    private final Clock clock;

    public RegisterService(UserRepository userRepository, IdGenerator idGenerator, PasswordHasher passwordHasher,
                           RegistrationPolicy registrationPolicy, Clock clock) {
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.passwordHasher = passwordHasher;
        this.registrationPolicy = registrationPolicy;
        this.clock = clock;
    }

    public UUID register(String email, String rawPassword) {
        String canonicalEmail = registrationPolicy.canonicalizeEmail(email);
        registrationPolicy.validatePassword(rawPassword);

        if (userRepository.findByEmail(canonicalEmail).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        // Hash before starting the repository's write transaction.
        String passwordHash = passwordHasher.hash(rawPassword);
        Instant now = clock.instant();
        User user = User.create(idGenerator.generate(), canonicalEmail, passwordHash, now);

        return userRepository.create(user).getId();
    }
}
