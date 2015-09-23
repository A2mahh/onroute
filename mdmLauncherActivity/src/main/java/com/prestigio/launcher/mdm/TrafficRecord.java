package com.prestigio.launcher.mdm;

import android.net.TrafficStats;

/**
 * Created by yanis on 26.3.14.
 */
public class TrafficRecord {
    long tx=0;
    long rx=0;
    String tag=null;

    TrafficRecord() {
        tx=TrafficStats.getTotalTxBytes();
        rx=TrafficStats.getTotalRxBytes();
    }

    TrafficRecord(int uid, String tag) {
        tx=TrafficStats.getUidTxBytes(uid);
        rx= TrafficStats.getUidRxBytes(uid);
        this.tag=tag;
    }
}
