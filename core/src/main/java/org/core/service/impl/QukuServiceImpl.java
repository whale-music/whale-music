package org.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.pojo.*;
import org.core.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("MusicService")
public class QukuServiceImpl implements QukuService {
    
    @Autowired
    private TbMusicService musicService;
    
    @Autowired
    private TbAlbumService albumService;
    
    @Autowired
    private TbMusicSingerService musicSingerService;
    
    @Autowired
    private TbSingerService singerService;
    
    /**
     * 音乐地址服务
     */
    @Autowired
    private TbMusicUrlService musicUrlService;
    
    @Autowired
    private TbUserAlbumService userAlbumService;
    
    @Autowired
    private TbAlbumSingerService albumSingerService;
    
    @Autowired
    private TbUserSingerService userSingerService;
    
    /**
     * 获取专辑信息
     */
    @Override
    public TbAlbumPojo getAlbumByMusicId(Long musicId) {
        List<TbAlbumPojo> albumListByMusicId = getAlbumListByMusicId(Collections.singletonList(musicId));
        if (CollUtil.isEmpty(albumListByMusicId)) {
            return null;
        }
        if (albumListByMusicId.size() != 1) {
            throw new BaseException(ResultCode.ALBUM_ERROR);
        }
        return albumListByMusicId.get(0);
    }
    
    @Override
    public TbAlbumPojo getAlbumByAlbumId(Long albumIds) {
        List<TbAlbumPojo> albumListByAlbumId = getAlbumListByAlbumId(Collections.singletonList(albumIds));
        if (CollUtil.isEmpty(albumListByAlbumId)) {
            return null;
        }
        if (albumListByAlbumId.size() != 1) {
            throw new BaseException(ResultCode.ALBUM_ERROR);
        }
        return albumListByAlbumId.get(0);
    }
    
    /**
     * 批量获取歌手信息
     * Long -> music ID
     */
    @Override
    public List<TbAlbumPojo> getAlbumListByMusicId(List<Long> musicIds) {
        if (CollUtil.isEmpty(musicIds)) {
            return Collections.emptyList();
        }
        List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getId, musicIds));
        List<Long> albumIds = list.stream().map(TbMusicPojo::getAlbumId).collect(Collectors.toList());
        return getAlbumListByAlbumId(albumIds);
    }
    
    /**
     * 通过专辑ID 获取专辑信息
     */
    @Override
    public List<TbAlbumPojo> getAlbumListByAlbumId(List<Long> albumIds) {
        return albumService.listByIds(albumIds);
    }
    
    /**
     * 批量获取歌手信息
     * Long -> music ID
     */
    @Override
    public List<TbSingerPojo> getSingerListByMusicId(List<Long> musicIds) {
        List<TbMusicSingerPojo> list = musicSingerService.list(Wrappers.<TbMusicSingerPojo>lambdaQuery().in(TbMusicSingerPojo::getMusicId, musicIds));
        return getTbSingerPojoList(CollUtil.isEmpty(list), list.stream().map(TbMusicSingerPojo::getSingerId));
    }
    
    /**
     * 获取歌手信息
     */
    @Override
    public List<TbSingerPojo> getSingerByMusicId(Long musicId) {
        return getSingerListByMusicId(Collections.singletonList(musicId));
    }
    
    /**
     * 获取歌曲URL下载地址
     */
    @Override
    public List<TbMusicUrlPojo> getMusicUrl(Long musicId) {
        return getMusicUrl(Set.of(musicId));
    }
    
    @Override
    public List<TbMusicUrlPojo> getMusicUrl(Set<Long> musicId) {
        LambdaQueryWrapper<TbMusicUrlPojo> in = Wrappers.<TbMusicUrlPojo>lambdaQuery().in(TbMusicUrlPojo::getMusicId, musicId);
        return musicUrlService.list(in);
    }
    
    /**
     * 随即获取曲库中的一条数据
     */
    @Override
    public TbMusicPojo randomMusic() {
        long count = musicService.count();
        Page<TbMusicPojo> page = new Page<>(RandomUtil.randomLong(0, count), 1);
        musicService.page(page);
        return Optional.ofNullable(page.getRecords()).orElse(new ArrayList<>()).get(0);
    }
    
    @Override
    public Page<TbAlbumPojo> getAlbumPage(String area, Long offset, Long limit) {
        Page<TbAlbumPojo> page = new Page<>(offset, limit);
        albumService.page(page);
        return page;
    }
    
    /**
     * 查询专辑下音乐数量
     *
     * @param albumId 专辑ID
     */
    @Override
    public Integer getAlbumMusicCountByAlbumId(Long albumId) {
        long count = albumService.count(Wrappers.<TbAlbumPojo>lambdaQuery().eq(TbAlbumPojo::getId, albumId));
        return Integer.valueOf(count + "");
    }
    
    /**
     * 查询专辑下音乐数量
     *
     * @param musicId 歌曲ID
     */
    @Override
    public Integer getAlbumMusicCountByMusicId(Long musicId) {
        TbMusicPojo tbMusicPojo = musicService.getOne(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getId, musicId));
        if (tbMusicPojo == null) {
            return null;
        }
        return getAlbumMusicCountByAlbumId(tbMusicPojo.getAlbumId());
    }
    
    /**
     * 获取歌手音乐数量
     *
     * @param id 歌手ID
     */
    @Override
    public Long getMusicCountBySingerId(Long id) {
        return musicSingerService.count(Wrappers.<TbMusicSingerPojo>lambdaQuery().eq(TbMusicSingerPojo::getSingerId, id));
    }
    
    /**
     * 获取专辑歌手列表
     */
    @Override
    public List<TbSingerPojo> getSingerListByAlbumIds(Long albumIds) {
        return getSingerListByAlbumIds(Collections.singletonList(albumIds));
    }
    
    /**
     * 获取专辑歌手列表
     */
    @Override
    public List<TbSingerPojo> getSingerListByAlbumIds(List<Long> albumIds) {
        List<TbAlbumSingerPojo> list = albumSingerService.list(Wrappers.<TbAlbumSingerPojo>lambdaQuery().in(TbAlbumSingerPojo::getAlbumId, albumIds));
        return getTbSingerPojoList(CollUtil.isEmpty(list), list.stream().map(TbAlbumSingerPojo::getSingerId));
    }
    
    private List<TbSingerPojo> getTbSingerPojoList(boolean empty, Stream<Long> longStream) {
        if (empty) {
            return Collections.emptyList();
        }
        List<Long> singerIds = longStream.collect(Collectors.toList());
        return singerService.list(Wrappers.<TbSingerPojo>lambdaQuery().in(TbSingerPojo::getId, singerIds));
    }
    
    /**
     * 查询用户收藏专辑
     *
     * @param user    用户数据
     * @param current 当前页数
     * @param size    每页多少数据
     */
    @Override
    public List<TbAlbumPojo> getUserCollectAlbum(SysUserPojo user, Long current, Long size) {
        List<TbUserAlbumPojo> userAlbumPojoList = userAlbumService.list(Wrappers.<TbUserAlbumPojo>lambdaQuery()
                                                                                .eq(TbUserAlbumPojo::getUserId, user.getId()));
        if (CollUtil.isEmpty(userAlbumPojoList)) {
            return Collections.emptyList();
        }
        List<Long> albumIds = userAlbumPojoList.stream().map(TbUserAlbumPojo::getAlbumId).collect(Collectors.toList());
        return getAlbumListByAlbumId(albumIds);
    }
    
    
    /**
     * 获取用户关注歌手
     *
     * @param user 用户信息
     */
    @Override
    public List<TbSingerPojo> getUserLikeSingerList(SysUserPojo user) {
        List<TbUserSingerPojo> userLikeSinger = userSingerService.list(Wrappers.<TbUserSingerPojo>lambdaQuery()
                                                                               .eq(TbUserSingerPojo::getUserId, user.getId()));
        if (CollUtil.isEmpty(userLikeSinger)) {
            return Collections.emptyList();
        }
        List<Long> singerIds = userLikeSinger.stream().map(TbUserSingerPojo::getSingerId).collect(Collectors.toList());
        return singerService.listByIds(singerIds);
    }
    
    /**
     * 获取歌手所有专辑数量
     *
     * @param id 歌手ID
     */
    @Override
    public Integer getAlbumCountBySingerId(Long id) {
        return Math.toIntExact(albumSingerService.count(Wrappers.<TbAlbumSingerPojo>lambdaQuery().eq(TbAlbumSingerPojo::getSingerId, id)));
    }
    
    /**
     * 根据专辑ID查找音乐
     *
     * @param id 专辑ID
     */
    @Override
    public List<TbMusicPojo> getMusicListByAlbumId(Long id) {
        return musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getAlbumId, id));
    }
    
    /**
     * 获取歌手下音乐信息
     *
     * @param id 歌手ID
     */
    @Override
    public List<TbMusicPojo> getMusicListBySingerId(Long id) {
        List<TbMusicSingerPojo> tbMusicSingerPojos = musicSingerService.list(Wrappers.<TbMusicSingerPojo>lambdaQuery()
                                                                                     .eq(TbMusicSingerPojo::getSingerId, id));
        Set<Long> collect = tbMusicSingerPojos.stream().map(TbMusicSingerPojo::getMusicId).collect(Collectors.toSet());
        return musicService.listByIds(collect);
    }
    
    /**
     * 随机获取歌手
     *
     * @param count 获取数量
     */
    @Override
    public List<TbSingerPojo> randomSinger(int count) {
        long sum = singerService.count();
        ArrayList<TbSingerPojo> res = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long randomNum = RandomUtil.randomLong(sum);
            Page<TbSingerPojo> page = singerService.page(new Page<>(randomNum, 1));
            res.addAll(page.getRecords());
        }
        return res;
    }
}
