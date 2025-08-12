package ru.rudn.rudnadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rudn.rudnadmin.entity.vpn.TaskType;
import ru.rudn.rudnadmin.service.vpn.VpnTaskFlow;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Configuration
public class VpnConfig {

    @Bean
    public Map<TaskType, VpnTaskFlow> vpnFlows(final List<VpnTaskFlow> taskFlowList) {
        return taskFlowList.stream().collect(toMap(VpnTaskFlow::getType, Function.identity()));
    }
}
