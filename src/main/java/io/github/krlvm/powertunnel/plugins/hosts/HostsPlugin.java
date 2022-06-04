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

import io.github.krlvm.powertunnel.sdk.configuration.Configuration;
import io.github.krlvm.powertunnel.sdk.plugin.PowerTunnelPlugin;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;
import io.github.krlvm.powertunnel.sdk.utiities.TextReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
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

        String raw = "";

        final Configuration config = readConfiguration();

        final String mirror = config.get("mirror", null);
        final long interval = getMirrorInterval(config.get("mirror_interval", "interval_2"));
        if (mirror != null && !mirror.trim().isEmpty()) {
            if ((System.currentTimeMillis() - config.getLong("last_mirror_load", 0)) < interval) {
                final String cached = loadHostsFromCache();
                if(cached == null) {
                    final String mirrored = loadHostsFromMirror(mirror, config, interval != 0);
                    if (mirrored != null) {
                        raw += mirrored + "\n";
                    }
                } else {
                    raw += cached + "\n";
                }
            } else {
                final String mirrored = loadHostsFromMirror(mirror, config, interval != 0);
                if (mirrored == null) {
                    final String cached = loadHostsFromCache();
                    if (cached != null) {
                        raw += cached + "\n";
                    }
                } else {
                    raw += mirrored + "\n";
                }
            }
        }

        final Map<String, InetAddress> hosts = new HashMap<>();

        try {
            raw += readTextFile("hosts.txt");
        } catch (IOException ex) {
            LOGGER.error("Failed to read hosts file: {}", ex.getMessage(), ex);
            return;
        }

        for (String line : raw.split("\n")) {
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

    private String loadHostsFromMirror(String mirror, Configuration config, boolean caching) {
        LOGGER.info("Loading Hosts file from mirror...");
        try {
            final String raw = TextReader.read(new URL(mirror).openStream());
            if (caching) {
                try {
                    config.setLong("last_mirror_load", System.currentTimeMillis());
                    saveConfiguration();
                } catch (IOException ex) {
                    LOGGER.warn("Failed to save the time of the last load of the Hosts file from the mirror: {}", ex.getMessage(), ex);
                }
                try {
                    saveTextFile("hosts-cache.txt", raw);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to save cached Hosts file: {}", ex.getMessage(), ex);
                }
            }
            return raw;
        } catch (IOException ex) {
            LOGGER.warn("Failed to load Hosts file from mirror: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private String loadHostsFromCache() {
        LOGGER.info("Loading mirrored Hosts file from cache...");
        try {
            return readTextFile("hosts-cache.txt");
        } catch (IOException ex) {
            LOGGER.error("Failed to read cached Hosts file: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static long getMirrorInterval(String key) {
        switch (key) {
            case "interval_5": return 3 * 24 * 60 * 60 * 1000;
            case "interval_4": return 2 * 24 * 60 * 60 * 1000;
            case "interval_3": return 24 * 60 * 60 * 1000;
            case "interval_1": return 0;
            default: case "interval_2": return 12 * 60 * 60 * 1000;
        }
    }
}
