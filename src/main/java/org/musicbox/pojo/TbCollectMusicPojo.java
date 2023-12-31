package org.musicbox.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 歌单和音乐的中间表，用于记录歌单中的每一个音乐
 * </p>
 *
 * @author Sakura
 * @since 2022-10-28
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_collect_music")
@ApiModel(value = "TbCollectMusicPojo对象", description = "歌单和音乐的中间表，用于记录歌单中的每一个音乐")
public class TbCollectMusicPojo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty("歌单ID")
    @TableId(value = "collect_id", type = IdType.AUTO)
    private Long collectId;
    
    @ApiModelProperty("音乐ID")
    @TableField(value = "music_id")
    private Long musicId;
    
    
}
