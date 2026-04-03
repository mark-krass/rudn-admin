package ru.rudn.rudnadmin.rest.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.rudn.rudnadmin.config.TestContainersBase;

import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KeycloakSecurityTest extends TestContainersBase {

    private static final String API_USERS_PATH = "/api/users";
    private static final String API_VPN_STUDENT_PATH = "/api/vpn/student/{studentId}";
    private static final String SWAGGER_PATH = "/v3/api-docs";
    private static final String REALM = "rudn";
    private static final String CLIENT_ID = "rudn-admin-api";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Security: swagger открыт без токена")
    void swagger_isPublic() throws Exception {
        mockMvc.perform(get(SWAGGER_PATH))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: API без токена возвращает 401")
    void api_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get(API_USERS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: MANAGER имеет доступ к /api/vpn/**")
    void manager_canAccessVpnApi() throws Exception {
        final String token = fetchToken("manager_user", "manager_pass");

        mockMvc.perform(get(API_VPN_STUDENT_PATH, 99999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security: KEYCLOAK_MANAGER не имеет доступа к /api/vpn/**")
    void keycloakManager_cannotAccessVpnApi() throws Exception {
        final String token = fetchToken("kc_manager_user", "kc_manager_pass");

        mockMvc.perform(get(API_VPN_STUDENT_PATH, 99999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: KEYCLOAK_MANAGER имеет доступ к /api/users/**")
    void keycloakManager_canAccessUsersApi() throws Exception {
        final String token = fetchToken("kc_manager_user", "kc_manager_pass");

        mockMvc.perform(get(API_USERS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: MANAGER не имеет доступа к /api/users/**")
    void manager_cannotAccessUsersApi() throws Exception {
        final String token = fetchToken("manager_user", "manager_pass");

        mockMvc.perform(get(API_USERS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: ADMIN имеет доступ и к /api/users/**, и к /api/vpn/**")
    void admin_canAccessUsersAndVpnApi() throws Exception {
        final String token = fetchToken("admin_user", "admin_pass");

        mockMvc.perform(get(API_USERS_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get(API_VPN_STUDENT_PATH, 99999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private String fetchToken(String username, String password) throws Exception {
        final URI authorizationUri = UriComponentsBuilder
                .fromUriString("http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080))
                .pathSegment("realms", REALM, "protocol", "openid-connect", "token")
                .build()
                .toUri();
        final RestClient restClient = RestClient.builder().build();

        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.put("grant_type", Collections.singletonList("password"));
        formData.put("client_id", Collections.singletonList(CLIENT_ID));
        formData.put("username", Collections.singletonList(username));
        formData.put("password", Collections.singletonList(password));

        final String result = restClient.post()
                .uri(authorizationUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class);

        assertNotNull(result, "Ответ token endpoint не должен быть пустым");

        final JsonNode tokenResponse = objectMapper.readTree(result);
        final JsonNode accessTokenNode = tokenResponse.get("access_token");
        assertNotNull(accessTokenNode, "access_token должен присутствовать в ответе Keycloak. body: " + result);
        return accessTokenNode.asText();
    }
}
