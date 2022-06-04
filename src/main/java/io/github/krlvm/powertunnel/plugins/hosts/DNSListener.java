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

import io.github.krlvm.powertunnel.sdk.proxy.DNSRequest;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public class DNSListener extends ProxyAdapter {

    private final Map<String, InetAddress> hosts;

    public DNSListener(Map<String, InetAddress> hosts) {
        this.hosts = hosts;
    }

    @Override
    public Boolean onResolutionRequest(@NotNull DNSRequest request) {
        final InetAddress address = hosts.get(request.getHost());
        if (address != null) {
            request.setResponse(new InetSocketAddress(address, request.getPort()));
        }
        return super.onResolutionRequest(request);
    }
}