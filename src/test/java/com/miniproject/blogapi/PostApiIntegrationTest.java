package com.miniproject.blogapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Logs in via the real /api/auth/login endpoint and returns just the
    // access token, ready to drop into an Authorization header. This
    // replaces httpBasic(...) everywhere below, since Basic Auth no
    // longer exists in this app at all.
    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String body = """
                { "username": "%s", "password": "%s" }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private Long createDraftPostAsUser() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        String body = """
                { "text": "Integration test post", "attachments": [], "remarks": "" }
                """;

        String response = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    @Test
    void login_withValidCredentials_returnsTokens() throws Exception {
        String body = """
                { "username": "user", "password": "userpass" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String body = """
                { "username": "user", "password": "wrongpassword" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_asAuthenticatedUser_returns201WithDraftStatus() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        String body = """
                { "text": "My integration test post", "attachments": [], "remarks": "note" }
                """;

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("user"));
    }

    @Test
    void createPost_withoutToken_returns401() throws Exception {
        String body = """
                { "text": "No auth attempt", "attachments": [], "remarks": "" }
                """;

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_withBlankText_returns400() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        String body = """
                { "text": "", "attachments": [], "remarks": "" }
                """;

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getPostById_nonexistentId_returns404() throws Exception {
        String token = loginAndGetAccessToken("admin", "adminpass");

        mockMvc.perform(get("/api/posts/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void publish_asRegularUser_returns403() throws Exception {
        Long id = createDraftPostAsUser();
        String userToken = loginAndGetAccessToken("user", "userpass");

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void publish_asAdmin_returns200AndPublishedStatus() throws Exception {
        Long id = createDraftPostAsUser();
        String adminToken = loginAndGetAccessToken("admin", "adminpass");

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void updatePost_afterPublish_asOwner_returns409() throws Exception {
        Long id = createDraftPostAsUser();
        String adminToken = loginAndGetAccessToken("admin", "adminpass");

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String userToken = loginAndGetAccessToken("user", "userpass");
        String updateBody = """
                { "text": "Trying to edit after publish", "attachments": [], "remarks": "" }
                """;

        mockMvc.perform(put("/api/posts/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isConflict());
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewAccessToken() throws Exception {
        String loginBody = """
                { "username": "user", "password": "userpass" }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        String refreshBody = """
                { "refreshToken": "%s" }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
