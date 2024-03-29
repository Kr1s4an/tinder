package com.volasoftware.tinder.service;

import com.volasoftware.tinder.dto.UserDto;
import com.volasoftware.tinder.exception.EmailAlreadyRegisteredException;
import com.volasoftware.tinder.exception.PasswordDoesNotMatchException;
import com.volasoftware.tinder.exception.UserDoesNotExistException;
import com.volasoftware.tinder.exception.UserIsNotVerifiedException;
import com.volasoftware.tinder.login.LoginUserDto;
import com.volasoftware.tinder.model.Gender;
import com.volasoftware.tinder.model.Role;
import com.volasoftware.tinder.model.User;
import com.volasoftware.tinder.model.Verification;
import com.volasoftware.tinder.repository.UserRepository;
import com.volasoftware.tinder.repository.VerificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final ResourceLoader resourceLoader;

    private JavaMailSender mailSender;

    public UserService(UserRepository userRepository,
                       VerificationRepository verificationRepository,
                       ResourceLoader resourceLoader,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.resourceLoader = resourceLoader;
        this.mailSender = mailSender;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    private String getEmailContent(String token) throws IOException {
        Resource emailResource = resourceLoader.getResource("classpath:email/registrationEmail.html");
        File emailFile = emailResource.getFile();
        Path path = Path.of(emailFile.getPath());
        String emailContent = Files.readString(path);

        return emailContent.replace("{{token}}", "http://localhost:8080/verify/" + token);
    }

    public void registerUser(UserDto userDto) throws IOException, MessagingException {

        if (isEmailRegistered(userDto.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email already exist!");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(userDto.getPassword()));
        user.setGender(Gender.valueOf(userDto.getGender()));
        user.setRole(Role.USER);
        userRepository.save(user);

        Verification token = new Verification();
        token.setUserId(user);
        token.setToken(UUID.randomUUID().toString());
        token.setCreatedDate(LocalDateTime.now());
        token.setExpirationDate(LocalDateTime.now().plusDays(2));
        verificationRepository.saveAndFlush(token);

        MimeMessage message = mailSender.createMimeMessage();
        message.setFrom(new InternetAddress("kristinmpetkov@gmail.com"));
        message.setRecipients(MimeMessage.RecipientType.TO, user.getEmail());
        message.setSubject("Verification");
        message.setContent(getEmailContent(token.getToken()), "text/html; charset=utf-8");
        mailSender.send(message);
    }

    public User loginUser(LoginUserDto input) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = userRepository.findOneByEmail(input.getEmail()).orElseThrow(
                () -> new UserDoesNotExistException("User with this email does not exist"));
        if (!passwordEncoder.matches(input.getPassword(),
                user.getPassword())) {
            throw new PasswordDoesNotMatchException("Password does not match");
        }
        if(!user.isVerified()){
            throw new UserIsNotVerifiedException("The email is not verified");
        }
        return user;
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.findOneByEmail(email).isPresent();
    }

    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return userRepository.findOneByEmail(username).orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
            }
        };
    }
}
