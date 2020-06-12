package alexiil.mc.lib.multipart.impl;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.server.ServerTickCallback;

/** Limited version of buildcraft's "buildcraft.lib.misc.MessageUtil" event queueing. */
final class DelayedMessageQueue {
    private DelayedMessageQueue() {}

    private static final List<Runnable> TASKS = new ArrayList<>();

    static void init() {
        ServerTickCallback.EVENT.register(server -> runTasks());
    }

    private static void runTasks() {
        for (Runnable task : TASKS) {
            task.run();
        }
        TASKS.clear();
    }

    static void appendTask(Runnable task) {
        TASKS.add(task);
    }
}
