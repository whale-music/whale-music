package org.musicbox.common.vo.neteasecloudmusic.playlistallsong;

import lombok.Data;

@Data
public class ChargeInfoListItem {
    private int rate;
    private Object chargeMessage;
    private int chargeType;
    private Object chargeUrl;
}