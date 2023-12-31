package org.musicbox.common.vo.neteasecloudmusic.playlist;

import lombok.Data;

import java.util.List;

@Data
public class PlayListVo {
	private int code;
	private List<PlaylistItem> playlist;
	private boolean more;
	private String version;
}