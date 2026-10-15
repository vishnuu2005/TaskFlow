package com.taskflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unauthenticatedAccessToAdmin_ShouldBeForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAccessToCourses_ShouldBeForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@taskflow.com", roles = {"STUDENT"})
    void studentAccessToAdminUsers_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher@taskflow.com", roles = {"TEACHER"})
    void teacherAccessToAdminUsers_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@taskflow.com", roles = {"ADMIN"})
    void adminAccessToAdminUsers_ShouldBeOk() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void publicRegistrationAsAdmin_ShouldBeRejected() throws Exception {
        RegisterRequest req = new RegisterRequest("Evil Admin", "evil@admin.com", "Password123!", "ADMIN");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicRegistrationAsStudent_ShouldSucceed() throws Exception {
        String email = "newstudent_" + System.currentTimeMillis() + "@test.com";
        RegisterRequest req = new RegisterRequest("New Student", email, "Password123!", "STUDENT");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void deactivatedUserLogin_ShouldBeRejected() throws Exception {
        String email = "deactivated_" + System.currentTimeMillis() + "@test.com";
        User deactivated = new User();
        deactivated.setName("Deactivated User");
        deactivated.setEmail(email);
        deactivated.setPassword(passwordEncoder.encode("Password123!"));
        deactivated.setRole(Role.STUDENT);
        deactivated.setActive(false);
        userRepository.save(deactivated);

        LoginRequest loginReq = new LoginRequest(email, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }
}
