package org.core.mybatis.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 歌曲专辑表
 * </p>
 *
 * @author Sakura
 * @since 2023-06-25
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_album")
@ApiModel(value = "TbAlbumPojo对象", description = "歌曲专辑表")
public class TbAlbumPojo extends Model<TbAlbumPojo> implements Serializable{

    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty("专辑表ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    @ApiModelProperty("专辑名")
    @TableField("album_name")
    private String albumName;
    
    @ApiModelProperty("专辑版本（比如录音室版，现场版）")
    @TableField("sub_type")
    private String subType;
    
    @ApiModelProperty("专辑简介")
    @TableField("description")
    private String description;
    
    @ApiModelProperty("发行公司")
    @TableField("company")
    private String company;
    
    @ApiModelProperty("专辑发布时间")
    @TableField("publish_time")
    private LocalDateTime publishTime;
    
    @ApiModelProperty("上传用户ID")
    @TableField("user_id")
    private Long userId;
    
    @ApiModelProperty("修改时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;


}