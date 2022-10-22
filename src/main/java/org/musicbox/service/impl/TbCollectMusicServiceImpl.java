package org.musicbox.service.impl;

import org.musicbox.pojo.TbCollectMusicPojo;
import org.musicbox.mapper.TbCollectMusicMapper;
import org.musicbox.service.TbCollectMusicService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 歌单和音乐的中间表，用于记录歌单中的每一个音乐 服务实现类
 * </p>
 *
 * @author Sakura
 * @since 2022-10-22
 */
@Service
public class TbCollectMusicServiceImpl extends ServiceImpl<TbCollectMusicMapper, TbCollectMusicPojo> implements TbCollectMusicService {

}
