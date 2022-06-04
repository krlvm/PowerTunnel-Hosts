/*
 * This file is part of PowerTunnel-Hosts.
 *
 * PowerTunnel-Hosts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Hosts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Hosts.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.plugins.hosts;

import io.github.krlvm.powertunnel.sdk.plugin.PowerTunnelPlugin;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class HostsPlugin extends PowerTunnelPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostsPlugin.class);

    @Override
    public void onProxyInitialization(@NotNull ProxyServer proxy) {
        if (!proxy.areHostnamesAvailable()) {
            LOGGER.warn("Hosts plugin is not available when VPN-level hostname resolving is enabled");
            return;
        }

        final Map<String, InetAddress> hosts = new HashMap<>();

        final String s;
        try {
            s = readTextFile("hosts.txt");
        } catch (IOException ex) {
            LOGGER.error("Failed to read hosts file: {}", ex.getMessage(), ex);
            return;
        }

        for (String line : s.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] arr = line.split("\\s+");
            if (arr.length != 2) {
                LOGGER.warn("Malformed line: '{}'", line);
                continue;
            }

            final InetAddress address;
            try {
                address = InetAddress.getByName(arr[1]);
            } catch (UnknownHostException exception) {
                LOGGER.warn("Invalid IP Address for '{}': '{}'", arr[0], arr[1]);
                continue;
            }

            hosts.put(arr[0].toLowerCase(), address);
        }

        if (hosts.isEmpty()) {
            LOGGER.warn("Hosts file is empty");
            return;
        }

        registerProxyListener(new DNSListener(hosts), -10);

        LOGGER.info("Loaded {} hosts", hosts.size());
    }
}
