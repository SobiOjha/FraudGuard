package com.FraudGaurd.fraudguard_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.model.User;
import com.FraudGaurd.fraudguard_backend.model.UserRole;
import com.FraudGaurd.fraudguard_backend.repository.IntegrationRepository;
import com.FraudGaurd.fraudguard_backend.repository.UserRepository;
import com.FraudGaurd.fraudguard_backend.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    void cleanDatabase() {
        integrationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void transactionEndpointRejectsMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("API key is required"));
    }

    @Test
    void transactionEndpointRejectsInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("X-API-Key", "invalid-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API key"));
    }

    @Test
    void transactionEndpointAcceptsValidApiKey() throws Exception {
        ApiKeyService.CreatedIntegration created =
                apiKeyService.createIntegration("Demo Payment System");

        mockMvc.perform(get("/api/transactions")
                        .header("X-API-Key", created.rawApiKey()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void transactionMutationUsesApiKeyWithoutCsrfToken() throws Exception {
        ApiKeyService.CreatedIntegration created =
                apiKeyService.createIntegration("Demo Payment System");

        mockMvc.perform(post("/api/transactions/analyze")
                        .header("X-API-Key", created.rawApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": "U1001",
                                    "amount": 1000,
                                    "currency": "INR",
                                    "recipient": "RECIPIENT-01",
                                    "transactionType": "TRANSFER",
                                    "location": "Delhi",
                                    "deviceId": "DEVICE-01",
                                    "protectionMode": "NORMAL"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveIntegrationIsRejected() throws Exception {
        ApiKeyService.CreatedIntegration created =
                apiKeyService.createIntegration("Revoked Integration");
        Integration integration = created.integration();
        integration.setActive(false);
        integrationRepository.save(integration);

        mockMvc.perform(get("/api/transactions")
                        .header("X-API-Key", created.rawApiKey()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API key"));
    }

    @Test
    void integrationManagementRequiresAdminAuthentication() throws Exception {
        CsrfCredentials csrfCredentials = issueCsrfToken();

        mockMvc.perform(post("/api/integrations")
                        .cookie(csrfCredentials.cookie())
                        .header("X-XSRF-TOKEN", csrfCredentials.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void integrationManagementRequiresCsrfToken() throws Exception {
        saveAdminUser();

        mockMvc.perform(post("/api/integrations")
                        .with(httpBasic("admin", "AdminPassword123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void adminCanObtainCsrfToken() throws Exception {
        saveAdminUser();

        mockMvc.perform(get("/api/integrations/csrf-token")
                        .with(httpBasic("admin", "AdminPassword123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void nonAdminUserCannotManageIntegrations() throws Exception {
        CsrfCredentials csrfCredentials = issueCsrfToken();
        saveUser("user", UserRole.USER, "UserPassword123");

        mockMvc.perform(post("/api/integrations")
                        .with(httpBasic("user", "UserPassword123"))
                        .cookie(csrfCredentials.cookie())
                        .header("X-XSRF-TOKEN", csrfCredentials.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void adminCanCreateIntegrationAndRawKeyIsOnlyInCreationResponse()
            throws Exception {
        CsrfCredentials csrfCredentials = issueCsrfToken();

        mockMvc.perform(post("/api/integrations")
                        .with(httpBasic("admin", "AdminPassword123"))
                        .cookie(csrfCredentials.cookie())
                        .header("X-XSRF-TOKEN", csrfCredentials.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo Payment System\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Demo Payment System"))
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.apiKeyHash").doesNotExist());

        Integration saved = integrationRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getApiKeyHash());
        org.junit.jupiter.api.Assertions.assertEquals(64, saved.getApiKeyHash().length());
    }

    @Test
    void adminCanUseIssuedCsrfTokenForIntegrationMutation() throws Exception {
        CsrfCredentials csrfCredentials = issueCsrfToken();

        mockMvc.perform(post("/api/integrations")
                        .with(httpBasic("admin", "AdminPassword123"))
                        .cookie(csrfCredentials.cookie())
                        .header("X-XSRF-TOKEN", csrfCredentials.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Issued Token Demo\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanDisableIntegration() throws Exception {
        CsrfCredentials csrfCredentials = issueCsrfToken();
        ApiKeyService.CreatedIntegration created =
                apiKeyService.createIntegration("Demo Payment System");

        mockMvc.perform(patch("/api/integrations/" + created.integration().getId() + "/status")
                        .with(httpBasic("admin", "AdminPassword123"))
                        .cookie(csrfCredentials.cookie())
                        .header("X-XSRF-TOKEN", csrfCredentials.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.apiKeyHash").doesNotExist());
    }

    @Test
    void legacyRegistrationEndpointIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"Password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void legacyLoginEndpointIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"Password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void swaggerDocumentsApiKeySecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("X-API-Key")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("apiKeyAuth")));
    }

    private void saveAdminUser() {
        saveUser("admin", UserRole.ADMIN, "AdminPassword123");
    }

    private CsrfCredentials issueCsrfToken() throws Exception {
        saveAdminUser();

        MvcResult csrfResult = mockMvc.perform(get("/api/integrations/csrf-token")
                        .with(httpBasic("admin", "AdminPassword123")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode csrfBody = objectMapper.readTree(
                csrfResult.getResponse().getContentAsString());
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        org.junit.jupiter.api.Assertions.assertNotNull(csrfCookie);

        return new CsrfCredentials(
                csrfCookie,
                csrfBody.get("token").asText()
        );
    }

    private record CsrfCredentials(Cookie cookie, String token) {
    }

    private void saveUser(
            String username,
            UserRole role,
            String password) {
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(role);
        userRepository.save(admin);
    }
}
