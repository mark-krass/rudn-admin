package ru.rudn.rudnadmin.service.openvpn.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.rudn.rudnadmin.service.openvpn.VpnService;
import ru.rudn.rudnadmin.service.openvpn.exception.OpenVpnException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class OpenVpnService implements VpnService {

    @Value("${vpn.scripts.create}")
    private String createScriptPath;

    @Value("${vpn.scripts.delete}")
    private String deleteScriptPath;

    @Override
    public void createRemoteVpn(final Long connectId) {
        executeScript(createScriptPath, connectId);
    }

    @Override
    public void removeRemoteVpn(final Long connectId) {
        executeScript(deleteScriptPath, connectId);
    }

    private void executeScript(final String scriptPath, final Long connectId) {
        final ProcessBuilder processBuilder = buildProcess(scriptPath, String.valueOf(connectId));
        try {
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new OpenVpnException("Script exited with non-zero code: " + exitCode);
            }
        } catch (Exception e) {
            throw new OpenVpnException("Failed to execute script: " + e.getMessage());
        }
    }

    private ProcessBuilder buildProcess(final String scriptPath, final String connectId) {
        final Path absoluteScript = Paths.get(scriptPath).toAbsolutePath().normalize();
        final String absoluteScriptString = absoluteScript.toString();
        final boolean isPs1 = absoluteScriptString.endsWith(".ps1");
        if (isPs1) {
            return new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    absoluteScriptString,
                    connectId
            );
        }

        return new ProcessBuilder(
                "/bin/bash",
                absoluteScriptString,
                connectId
        );
    }
}
