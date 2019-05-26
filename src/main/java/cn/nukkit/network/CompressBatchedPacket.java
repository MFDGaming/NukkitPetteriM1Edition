package cn.nukkit.network;

import cn.nukkit.Server;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Zlib;

import java.util.List;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class CompressBatchedPacket extends AsyncTask {

    public int level;
    public byte[] data;
    public byte[] finalData;
    public int channel;
    public List<String> targets;

    public CompressBatchedPacket(byte[] data, List<String> targets) {
        this(data, targets, 7);
    }

    public CompressBatchedPacket(byte[] data, List<String> targets, int level) {
        this(data, targets, level, 0);
    }

    public CompressBatchedPacket(byte[] data, List<String> targets, int level, int channel) {
        this.data = data;
        this.targets = targets;
        this.level = level;
        this.channel = channel;
    }

    @Override
    public void onRun() {
        try {
            this.finalData = Zlib.deflate(data, level);
            this.data = null;
        } catch (Exception e) {
        }
    }

    @Override
    public void onCompletion(Server server) {
        server.broadcastPacketsCallback(this.finalData, this.targets);
    }
}
