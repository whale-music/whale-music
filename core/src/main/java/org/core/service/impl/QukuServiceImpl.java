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
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<Long> collect = list.stream().map(TbMusicSingerPojo::getSingerId).collect(Collectors.toList());
        return singerService.list(Wrappers.<TbSingerPojo>lambdaQuery().in(TbSingerPojo::getId, collect));
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
    
    
    /**
     * 查询专辑下音乐数量
     *
     * @param albumId 专辑ID
     */
    @Override
    public Integer getAlbumMusicSizeByAlbumId(Long albumId) {
        long count = albumService.count(Wrappers.<TbAlbumPojo>lambdaQuery().eq(TbAlbumPojo::getId, albumId));
        return Integer.valueOf(count + "");
    }
    
    /**
     * 查询专辑下音乐数量
     *
     * @param musicId 歌曲ID
     */
    @Override
    public Integer getAlbumMusicSizeByMusicId(Long musicId) {
        TbMusicPojo tbMusicPojo = musicService.getOne(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getId, musicId));
        if (tbMusicPojo == null) {
            return null;
        }
        return getAlbumMusicSizeByAlbumId(tbMusicPojo.getAlbumId());
    }
}
