package org.core.jpa.model.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("音乐专辑歌单封面表")
public class TbPicEntityDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    
    
    /**
     * 音乐网络地址，或路径
     */
    @ApiModelProperty("音乐网络地址，或路径")
    private String url;
    
    private String md5;
    
    
    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    private Date updateTime;
    
    
    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;
    
}
