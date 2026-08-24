package com.miniproject.blogapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.blogapi.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private CloudinaryService cloudinaryService;

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

        String response = mockMvc.perform(multipart("/api/posts")
                        .param("text", "Integration test post")
                        .header("Authorization", "Bearer " + token))
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
                .andExpect(jsonPath("$.refreshToken").exists());
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
    void createPost_asAuthenticatedUser_withNoFile_returns201WithDraftStatus() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        mockMvc.perform(multipart("/api/posts")
                        .param("text", "My integration test post")
                        .param("remarks", "note")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("user"))
                .andExpect(jsonPath("$.attachments").isEmpty());
    }

    @Test
    void createPost_withFile_uploadsViaCloudinaryAndStoresUrl() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        MockMultipartFile fakeImage = new MockMultipartFile(
                "files", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        when(cloudinaryService.uploadFile(any(MultipartFile.class)))
                .thenReturn("https://res.cloudinary.com/fake-cloud/photo.jpg");

        mockMvc.perform(multipart("/api/posts")
                        .file(fakeImage)
                        .param("text", "Post with a real-looking attachment")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0]").value("https://res.cloudinary.com/fake-cloud/photo.jpg"));
    }

    @Test
    void createPost_whenCloudinaryFails_stillReturns201WithErrorReported() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        MockMultipartFile fakeImage = new MockMultipartFile(
                "files", "broken.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        when(cloudinaryService.uploadFile(any(MultipartFile.class)))
                .thenThrow(new com.miniproject.blogapi.exception.AttachmentUploadException(
                        "Simulated Cloudinary outage", new RuntimeException()));

        mockMvc.perform(multipart("/api/posts")
                        .file(fakeImage)
                        .param("text", "Post whose attachment will fail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments").isEmpty())
                .andExpect(jsonPath("$.attachmentUploadErrors").isNotEmpty());
    }

    @Test
    void createPost_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/api/posts")
                        .param("text", "No auth attempt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_withBlankText_returns400() throws Exception {
        String token = loginAndGetAccessToken("user", "userpass");

        mockMvc.perform(multipart("/api/posts")
                        .param("text", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
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
