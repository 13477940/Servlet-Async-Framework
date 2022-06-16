package framework.thread;

public class VirtualThreadStatic {

    public static void execute( Runnable runnable ) {
        VirtualThread vt = new VirtualThread();
        vt.execute(runnable);
    }

    private static class VirtualThread extends framework.thread.kotlin.VirtualThread {}

}
