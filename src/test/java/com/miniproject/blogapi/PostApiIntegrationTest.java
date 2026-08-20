package com.miniproject.blogapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createDraftPostAsUser() throws Exception {
        String body = """
                { "text": "Integration test post", "attachments": [], "remarks": "" }
                """;

        String response = mockMvc.perform(post("/api/posts")
                        .with(httpBasic("user", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    @Test
    void createPost_asAuthenticatedUser_returns201WithDraftStatus() throws Exception {
        String body = """
                { "text": "My integration test post", "attachments": [], "remarks": "note" }
                """;

        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("user", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("user"));
    }

    @Test
    void createPost_withoutCredentials_returns401() throws Exception {
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
        String body = """
                { "text": "", "attachments": [], "remarks": "" }
                """;

        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("user", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getPostById_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/999999")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publish_asRegularUser_returns403() throws Exception {
        Long id = createDraftPostAsUser();

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .with(httpBasic("user", "userpass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publish_asAdmin_returns200AndPublishedStatus() throws Exception {
        Long id = createDraftPostAsUser();

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void updatePost_afterPublish_asOwner_returns409() throws Exception {
        Long id = createDraftPostAsUser();

        mockMvc.perform(patch("/api/posts/" + id + "/publish")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk());

        String updateBody = """
                { "text": "Trying to edit after publish", "attachments": [], "remarks": "" }
                """;

        mockMvc.perform(put("/api/posts/" + id)
                        .with(httpBasic("user", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isConflict());
    }
}
