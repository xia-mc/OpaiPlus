package asia.lira.opaiplus.modules.misc.partyct;

public final class OpCode {
    public static final int HEARTBEAT = 1;  // 发送心跳（包括初次加入）
    public static final int LEAVE = 2;      // 退出 PartyCT
    public static final int REQUEST = 4;    // 请求当前频道的 PartyCT 信息（所有玩家立刻发送 HEARTBEAT）
}
