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
 * 音乐专辑歌单封面表
 * </p>
 *
 * @author Sakura
 * @since 2023-06-25
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_pic")
@ApiModel(value = "TbPicPojo对象", description = "音乐专辑歌单封面表")
public class TbPicPojo extends Model<TbPicPojo> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    @ApiModelProperty("音乐网络地址，或路径")
    @TableField(value = "url", fill = FieldFill.UPDATE)
    private String url;
    
    @ApiModelProperty("md5")
    @TableField("md5")
    private String md5;
    
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @ApiModelProperty("更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}