package org.musicbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.musicbox.pojo.TbMusicUrlPojo;

/**
 * <p>
 * 音乐下载地址 Mapper 接口
 * </p>
 *
 * @author Sakura
 * @since 2022-10-28
 */
@Mapper
public interface TbMusicUrlMapper extends BaseMapper<TbMusicUrlPojo> {

}
