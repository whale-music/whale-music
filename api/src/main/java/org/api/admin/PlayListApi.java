package org.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.api.admin.dto.MusicDto;
import org.api.admin.vo.MusicVo;
import org.core.pojo.*;
import org.core.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlayListApi {
    
    /**
     * 歌单表
     */
    @Autowired
    private TbCollectService collectService;
    
    /**
     * 音乐表
     */
    @Autowired
    private TbMusicService musicService;
    
    /**
     * 歌手表
     */
    @Autowired
    private TbSingerService singerService;
    
    @Autowired
    private TbMusicSingerService musicSingerService;
    
    /**
     * 专辑表
     */
    @Autowired
    private TbAlbumService albumService;
    
    /**
     * 音乐地址
     */
    @Autowired
    private TbMusicUrlService musicUrlService;
    
    
    /**
     * 歌单与音乐中间表
     */
    @Autowired
    private TbCollectMusicService collectMusicService;
    
    public Page<MusicVo> getAllMusicPage(MusicDto req) {
        Page<TbMusicPojo> page = new Page<>(req.getPageIndex(), req.getPageNum());
        
        // 查询歌手表
        List<TbMusicSingerPojo> musicSingerList = new ArrayList<>();
        List<TbSingerPojo> singerList = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getSingerName())) {
            singerList = singerService.list(Wrappers.<TbSingerPojo>lambdaQuery().like(TbSingerPojo::getSingerName, req.getSingerName()));
            List<Long> singerIdsList = singerList.stream().map(TbSingerPojo::getId).collect(Collectors.toList());
            musicSingerList = musicSingerService.listByIds(singerIdsList);
        }
        List<Long> musicSingerIds = musicSingerList.stream().map(TbMusicSingerPojo::getMusicId).collect(Collectors.toList());
        // 查询专辑表
        List<TbAlbumPojo> albumList = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getAlbumName())) {
            albumList = albumService.list(Wrappers.<TbAlbumPojo>lambdaQuery().like(TbAlbumPojo::getAlbumName, req.getAlbumName()));
        }
        LambdaQueryWrapper<TbMusicPojo> musicWrapper = new LambdaQueryWrapper<>();
        // 音乐ID
        musicWrapper.in(req.getIds() != null && !req.getIds().isEmpty(), TbMusicPojo::getId, req.getIds());
        // 音乐名
        musicWrapper.like(StringUtils.isNotBlank(req.getMusicName()), TbMusicPojo::getMusicName, req.getMusicName());
        // 别名
        musicWrapper.like(StringUtils.isNotBlank(req.getAliaName()), TbMusicPojo::getAliaName, req.getAliaName());
        // 歌手
        musicWrapper.in(StringUtils.isNotBlank(req.getSingerName()) && !musicSingerIds.isEmpty(), TbMusicPojo::getId, musicSingerIds);
        // 专辑
        musicWrapper.in(StringUtils.isNotBlank(req.getAlbumName()) && !albumList.isEmpty(), TbMusicPojo::getAlbumId, albumList);
        // 排序
        // sort歌曲添加顺序, createTime创建日期顺序,updateTime修改日期顺序, id歌曲ID顺序
        switch (req.getOrderBy()) {
            case "id":
                musicWrapper.orderBy(req.getOrder() != null, req.getOrder(), TbMusicPojo::getId);
                break;
            case "updateTime":
                musicWrapper.orderBy(req.getOrder() != null, req.getOrder(), TbMusicPojo::getUpdateTime);
                break;
            case "createTime":
                musicWrapper.orderBy(req.getOrder() != null, req.getOrder(), TbMusicPojo::getCreateTime);
                break;
            case "sort":
            default:
                musicWrapper.orderBy(req.getOrder() != null, req.getOrder(), TbMusicPojo::getSort);
        }
        
        musicService.page(page, musicWrapper);
        
        List<Long> musicUrl = page.getRecords().stream().map(TbMusicPojo::getId).collect(Collectors.toList());
        List<TbMusicUrlPojo> musicUrlList = musicUrlService.list(Wrappers.<TbMusicUrlPojo>lambdaQuery().in(TbMusicUrlPojo::getMusicId, musicUrl));
        List<MusicVo> musicVoList = new ArrayList<>();
        for (TbMusicPojo musicPojo : page.getRecords()) {
            MusicVo m = new MusicVo();
            // 替换歌手表
            Optional<TbMusicSingerPojo> musicSinger = musicSingerList.stream()
                                                                     .filter(tbMusicSingerPojo -> Objects.equals(musicPojo.getId(),
                                                                             tbMusicSingerPojo.getMusicId()))
                                                                     .findFirst();
            TbMusicSingerPojo tbMusicSingerPojo = musicSinger.orElse(new TbMusicSingerPojo());
            List<TbSingerPojo> tbSingerPojoList = singerList.stream()
                                                            .filter(singerPojo -> Objects.equals(tbMusicSingerPojo.getSingerId(), singerPojo.getId()))
                                                            .collect(Collectors.toList());
            
            // 替换专辑表
            Optional<TbAlbumPojo> first = albumList.stream()
                                                   .filter(tbAlbumPojo -> Objects.equals(tbAlbumPojo.getId(), musicPojo.getAlbumId()))
                                                   .findFirst();
            m.setSingerList(tbSingerPojoList);
            m.setAlbum(first.orElse(new TbAlbumPojo()));
            
            // 查询当前歌曲是否可播放
            List<TbMusicUrlPojo> musicUrls = musicUrlList.stream()
                                                         .filter(tbMusicUrlPojo -> Objects.equals(musicPojo.getId(), tbMusicUrlPojo.getMusicId()))
                                                         .collect(Collectors.toList());
            if (musicUrls.isEmpty()) {
                m.setIsPlaying(false);
            } else {
                m.setMusicUrlList(musicUrls);
                m.setIsPlaying(true);
            }
            musicVoList.add(m);
        }
        Page<MusicVo> voPage = new Page<>();
        BeanUtils.copyProperties(page, voPage);
        voPage.setRecords(musicVoList);
        return voPage;
    }
    
    public Page<MusicVo> getPlaylist(String playId, MusicDto req) {
        List<TbCollectMusicPojo> collectMusicList = collectMusicService.list(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                                                     .eq(TbCollectMusicPojo::getCollectId, playId));
        List<Long> musicIds = collectMusicList.stream().map(TbCollectMusicPojo::getMusicId).collect(Collectors.toList());
        req.setIds(musicIds);
        return getAllMusicPage(req);
    }
}
