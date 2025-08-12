package ru.rudn.rudnadmin.service.vpn;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.rudn.rudnadmin.service.openvpn.impl.OpenVpnService;

import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {OpenVpnService.class})
class OpenVpnServiceTest {

    private static boolean isWindows() {
        final String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    @Test
    @DisplayName("OpenVpnServiceImpl executes PS scripts: create returns resolved path and delete removes file")
    void createAndDelete_withPowershellScripts_success() throws Exception {
        Assumptions.assumeTrue(isWindows(), "Skipping: PowerShell expected on Windows");

        final URL createUrl = getClass().getResource("/scripts/create-vpn.ps1");
        final URL deleteUrl = getClass().getResource("/scripts/delete-vpn.ps1");
        assertNotNull(createUrl, "create-vpn.ps1 not found in test resources");
        assertNotNull(deleteUrl, "delete-vpn.ps1 not found in test resources");

        final Path createPath = Paths.get(createUrl.toURI());
        final Path deletePath = Paths.get(deleteUrl.toURI());
        final Path outputDir = createPath.getParent().resolve("out");
        Files.createDirectories(outputDir);

        final OpenVpnService service = new OpenVpnService();
        inject(service, "createScriptPath", createPath.toAbsolutePath().toString());
        inject(service, "deleteScriptPath", deletePath.toAbsolutePath().toString());

        final Long connectId = 777L;
        final String expectedPath = outputDir.resolve(connectId + ".ovpn").toAbsolutePath().toString();

        service.createRemoteVpn(connectId);
        assertTrue(Files.exists(Paths.get(expectedPath)), "Created file should exist: " + expectedPath);

        service.removeRemoteVpn(connectId);
        assertFalse(Files.exists(Paths.get(expectedPath)), "File should be deleted: " + expectedPath);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
