package ru.rudn.rudnadmin.rest.vpn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rudn.rudnadmin.config.TestContainersBase;
import ru.rudn.rudnadmin.service.vpn.scheduler.VpnTaskScheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", roles = {"ADMIN"})
class VpnSchedulerControllerTest extends TestContainersBase {

    private static final String PATH = "/api/vpn/scheduler";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VpnTaskScheduler scheduler;

    @Test
    @DisplayName("Scheduler: POST вызывает единый запуск createVpnForAllTasks")
    void runCreate() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(scheduler, times(1)).createVpnForAllTasks();
    }
}

