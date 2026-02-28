package practical.task.userservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import practical.task.userservice.dto.request.userDto.UserCreateDto;
import practical.task.userservice.dto.request.userDto.UserUpdateDto;
import practical.task.userservice.dto.response.UserResponse;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("userdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/user";
    }

    @Test
    void testFullUserFlow() {

        //given
        UserCreateDto createDto = new UserCreateDto(
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john.doe@example.com"
        );

        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                baseUrl + "/registration",
                createDto,
                UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());

        Long userId = createResponse.getBody().id();

        //when
        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                baseUrl + "/get/" + userId,
                UserResponse.class
        );

        UserUpdateDto updateDto = new UserUpdateDto(
                "Johnny",
                null,
                null,
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserUpdateDto> updateRequest = new HttpEntity<>(updateDto, headers);

        ResponseEntity<UserResponse> updateResponse = restTemplate.exchange(
                baseUrl + "/update/" + userId,
                HttpMethod.PATCH,
                updateRequest,
                UserResponse.class
        );

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/delete/" + userId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        ResponseEntity<UserResponse> afterDeleteResponse = restTemplate.getForEntity(
                baseUrl + "/get/" + userId,
                UserResponse.class
        );

        //then
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("John", getResponse.getBody().name());

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Johnny", updateResponse.getBody().name());

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        assertEquals(HttpStatus.OK, afterDeleteResponse.getStatusCode());
        assertFalse(afterDeleteResponse.getBody().active());
    }
}


