package cn.nukkit;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.message.*;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;

import java.net.Proxy;

public class JavaEditionServer {

    private cn.nukkit.Server nukkit;
    private String host;
    private static final int port = 25565;
    private static final Proxy PROXY = Proxy.NO_PROXY;
    private static final Proxy AUTH_PROXY = Proxy.NO_PROXY;
    private static final boolean VERIFY_USERS = false;

    public JavaEditionServer(cn.nukkit.Server nukkit) {
        this.nukkit = nukkit;
        this.host = nukkit.getPropertyString("server-ip", "0.0.0.0");

        Server server = new Server(host, port, MinecraftProtocol.class, new TcpSessionFactory(PROXY));
        server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, AUTH_PROXY);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, VERIFY_USERS);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(
                new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                new PlayerInfo(nukkit.getMaxPlayers(), nukkit.getOnlinePlayers().size(), new GameProfile[0]),
                new TextMessage(nukkit.getMotd()), null
        ));

        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
            nukkit.getLogger().info("Java Edition Server: [" + session.getHost() + ":" + session.getPort() + "] connected");
            session.send(new ServerJoinGamePacket(0, nukkit.isHardcore(), GameMode.SURVIVAL, nukkit.getDefaultLevel().getDimension(), nukkit.getMaxPlayers(), WorldType.DEFAULT, nukkit.getViewDistance(), false));
        });

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);
        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                nukkit.getLogger().info("Java Edition Server: Server closed");
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(PacketReceivedEvent event) {
                        if (event.getPacket() instanceof ClientChatPacket) {
                            ClientChatPacket packet = event.getPacket();
                            GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
                            System.out.println(profile.getName() + ": " + packet.getMessage());
                            event.getSession().send(new ServerChatPacket(packet.getMessage()));
                        }
                    }
                });
            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
                MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
                if (protocol.getSubProtocol() == SubProtocol.GAME) {
                    nukkit.getLogger().info("Java Edition Server: [" + event.getSession().getHost() + ":" + event.getSession().getPort() + "] disconnected");
                }
            }
        });

        server.bind();
        nukkit.getLogger().info("Java Edition Server: Server started on " + host + ":" + port);
    }
}
