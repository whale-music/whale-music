package org.core.jpa.model.vo;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@ApiModel("更新 字典数据表")
@EqualsAndHashCode(callSuper = false)
public class SysDictDataEntityUpdateVO extends SysDictDataEntityVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
}
