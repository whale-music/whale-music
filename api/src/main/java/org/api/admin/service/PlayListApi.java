package org.api.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.api.admin.model.common.PageCommon;
import org.api.admin.model.req.MusicAllReq;
import org.api.admin.model.req.MusicPageReq;
import org.api.admin.model.res.MusicPageRes;
import org.api.admin.model.res.MusicRes;
import org.api.admin.utils.MyPageUtil;
import org.core.config.SaveConfig;
import org.core.pojo.*;
import org.core.service.*;
import org.oss.factory.OSSFactory;
import org.oss.service.OSSService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("PlayListApi")
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
     * 音乐地址服务
     */
    @Autowired
    private TbMusicUrlService musicUrlService;
    
    /**
     * 歌单与音乐中间表
     */
    @Autowired
    private TbCollectMusicService collectMusicService;
    
    /**
     * 访问音乐文件地址
     */
    @Autowired
    private SaveConfig config;
    
    private static void pageOrderBy(boolean order, String orderBy, LambdaQueryWrapper<TbMusicPojo> musicWrapper) {
        // sort歌曲添加顺序, createTime创建日期顺序,updateTime修改日期顺序, id歌曲ID顺序
        switch (Optional.ofNullable(orderBy).orElse("")) {
            case "id":
                musicWrapper.orderBy(true, order, TbMusicPojo::getId);
                break;
            case "updateTime":
                musicWrapper.orderBy(true, order, TbMusicPojo::getUpdateTime);
                break;
            case "createTime":
                musicWrapper.orderBy(true, order, TbMusicPojo::getCreateTime);
                break;
            case "sort":
            default:
                musicWrapper.orderBy(true, order, TbMusicPojo::getSort);
        }
    }
    
    
    public Page<MusicRes> getAllMusic(MusicAllReq req) {
        req.getPage().setPageIndex(Optional.ofNullable(req.getPage().getPageIndex()).orElse(0));
        req.getPage().setPageNum(Optional.ofNullable(req.getPage().getPageNum()).orElse(200));
        
        Page<TbMusicPojo> page = new Page<>(req.getPage().getPageIndex(), req.getPage().getPageNum());
        
        // 查询歌手表
        List<TbSingerPojo> singerList;
        List<Long> musicSingerIds = null;
        if (StringUtils.isNotBlank(req.getSingerName())) {
            singerList = singerService.list(Wrappers.<TbSingerPojo>lambdaQuery().like(TbSingerPojo::getSingerName, req.getSingerName()));
            List<Long> singerIdsList = singerList.stream().map(TbSingerPojo::getId).collect(Collectors.toList());
            if (IterUtil.isNotEmpty(singerIdsList)) {
                List<TbMusicSingerPojo> musicSingerList = musicSingerService.listByIds(singerIdsList);
                musicSingerIds = musicSingerList.stream().map(TbMusicSingerPojo::getMusicId).collect(Collectors.toList());
            }
        }
        // 查询专辑表
        List<TbAlbumPojo> albumList = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getAlbumName())) {
            albumList = albumService.list(Wrappers.<TbAlbumPojo>lambdaQuery().like(TbAlbumPojo::getAlbumName, req.getAlbumName()));
        }
        LambdaQueryWrapper<TbMusicPojo> musicWrapper = new LambdaQueryWrapper<>();
        // 音乐ID
        musicWrapper.in(IterUtil.isNotEmpty(req.getMusicIds()), TbMusicPojo::getId, req.getMusicIds());
        // 音乐名
        musicWrapper.like(StringUtils.isNotBlank(req.getMusicName()), TbMusicPojo::getMusicName, req.getMusicName());
        // 别名
        musicWrapper.like(StringUtils.isNotBlank(req.getAliaName()), TbMusicPojo::getAliaName, req.getAliaName());
        // 歌手
        musicWrapper.in(StringUtils.isNotBlank(req.getSingerName()) && IterUtil.isNotEmpty(musicSingerIds), TbMusicPojo::getId, musicSingerIds);
        // 专辑
        musicWrapper.in(StringUtils.isNotBlank(req.getAlbumName()) && IterUtil.isNotEmpty(albumList), TbMusicPojo::getAlbumId, albumList);
        // 排序
        // sort歌曲添加顺序, createTime创建日期顺序,updateTime修改日期顺序, id歌曲ID顺序
        pageOrderBy(req.getOrder(), req.getOrderBy(), musicWrapper);
        
        musicService.page(page, musicWrapper);
        
        List<Long> musicIds = page.getRecords().stream().map(TbMusicPojo::getId).collect(Collectors.toList());
        
        // 开始填充音乐信息
        // 获取专辑
        List<Long> albumIds = page.getRecords().stream().map(TbMusicPojo::getAlbumId).collect(Collectors.toList());
        List<TbAlbumPojo> tbAlbumPojos = new ArrayList<>();
        if (!albumIds.isEmpty()) {
            tbAlbumPojos = albumService.listByIds(albumIds);
        }
        List<MusicRes> musicResList = new ArrayList<>();
        for (TbMusicPojo musicPojo : page.getRecords()) {
            MusicRes m = new MusicRes();
            BeanUtils.copyProperties(musicPojo, m);
    
            List<TbMusicSingerPojo> tbMusicSingerPojoList = new ArrayList<>();
            List<TbSingerPojo> tbSingerPojos = new ArrayList<>();
            if (!musicIds.isEmpty()) {
                // 获取歌曲地址
                List<TbMusicUrlPojo> musicUrlList = musicUrlService.list(Wrappers.<TbMusicUrlPojo>lambdaQuery().in(TbMusicUrlPojo::getMusicId, musicIds));
                // 获取歌手信息
                tbMusicSingerPojoList = musicSingerService.listByIds(musicIds);
        
                if (CollUtil.isNotEmpty(tbMusicSingerPojoList)) {
                    tbSingerPojos = singerService.listByIds(tbMusicSingerPojoList.stream()
                                                                                 .map(TbMusicSingerPojo::getSingerId)
                                                                                 .collect(Collectors.toList()));
                }
                // 查询当前歌曲是否可播放
                List<TbMusicUrlPojo> musicUrls = musicUrlList.stream()
                                                             .filter(tbMusicUrlPojo -> Objects.equals(musicPojo.getId(), tbMusicUrlPojo.getMusicId()))
                                                             .peek(tbMusicUrlPojo -> {
                                                                 try {
                                                                     OSSService aList = OSSFactory.ossFactory("AList");
                                                                     String musicAddresses = aList.getMusicAddresses(config.getHost(),
                                                                             config.getObjectSave(),
                                                                             tbMusicUrlPojo.getMd5() + "." + tbMusicUrlPojo.getEncodeType());
                                                                     tbMusicUrlPojo.setUrl(musicAddresses);
                                                                 } catch (Exception e) {
                                                                     tbMusicUrlPojo.setUrl("");
                                                                 }
                                                             })
                                                             .collect(Collectors.toList());
        
                if (musicUrls.isEmpty()) {
                    m.setIsPlaying(false);
                } else {
                    m.setMusicUrlList(musicUrls);
                    m.setIsPlaying(true);
                }
            }
    
            // 在返回音乐数据添加歌手信息
            Optional<TbMusicSingerPojo> musicSinger = tbMusicSingerPojoList.stream()
                                                                           .filter(tbMusicSingerPojo -> Objects.equals(musicPojo.getId(),
                                                                                   tbMusicSingerPojo.getMusicId()))
                                                                           .findFirst();
            TbMusicSingerPojo tbMusicSingerPojo = musicSinger.orElse(new TbMusicSingerPojo());
            // 歌手信息
            List<TbSingerPojo> tbSingerPojoList = tbSingerPojos.stream()
                                                               .filter(singerPojo -> Objects.equals(tbMusicSingerPojo.getSingerId(), singerPojo.getId()))
                                                               .collect(Collectors.toList());
            m.setSingerList(tbSingerPojoList);
            // 在返回音乐数据添加专辑表信息
            Optional<TbAlbumPojo> first = tbAlbumPojos.stream()
                                                      .filter(tbAlbumPojo -> Objects.equals(tbAlbumPojo.getId(), musicPojo.getAlbumId()))
                                                      .findFirst();
            m.setAlbum(first.orElse(new TbAlbumPojo()));
    
            musicResList.add(m);
        }
        Page<MusicRes> voPage = new Page<>();
        BeanUtils.copyProperties(page, voPage);
        voPage.setRecords(musicResList);
        return voPage;
    }
    
    public Page<MusicRes> getPlaylist(String playId, MusicAllReq req) {
        List<TbCollectMusicPojo> collectMusicList = collectMusicService.list(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                                                     .eq(TbCollectMusicPojo::getCollectId, playId));
        List<Long> musicIds = collectMusicList.stream().map(TbCollectMusicPojo::getMusicId).collect(Collectors.toList());
        req.setMusicIds(musicIds);
        return getAllMusic(req);
    }
    
    /**
     * 获取音乐基本信息
     */
    public Page<MusicPageRes> getMusicPage(MusicPageReq req) {
        req.setPage(MyPageUtil.checkPage(Optional.ofNullable(req.getPage()).orElse(new PageCommon())));
        List<Long> musicIdList = new LinkedList<>();
        
        // 查询歌手表
        musicIdList.addAll(getMusicIDBySingerName(req));
        // 查询专辑表
        musicIdList.addAll(getMusicIDByAlbumName(req));
        // 查询歌曲名
        musicIdList.addAll(getMusicIdByMusicName(req));
        
        Page<TbMusicPojo> page = new Page<>(req.getPage().getPageIndex(), req.getPage().getPageNum());
        LambdaQueryWrapper<TbMusicPojo> wrapper = Wrappers.<TbMusicPojo>lambdaQuery()
                                                          .in(CollUtil.isNotEmpty(musicIdList), TbMusicPojo::getId, musicIdList);
        pageOrderBy(req.getOrder(), req.getOrderBy(), wrapper);
        musicService.page(page, wrapper);
        
        
        // 专辑信息
        List<Long> albumIds = page.getRecords().stream().map(TbMusicPojo::getAlbumId).collect(Collectors.toList());
        Map<Long, TbAlbumPojo> albumMap = new HashMap<>();
        if (CollUtil.isNotEmpty(albumIds)) {
            albumMap = albumService.listByIds(albumIds)
                                   .stream()
                                   .collect(Collectors.toMap(TbAlbumPojo::getId, tbAlbumPojo -> tbAlbumPojo));
        }
        
        // 音乐ID
        List<Long> musicIds = page.getRecords().stream().map(TbMusicPojo::getId).collect(Collectors.toList());
        // 歌手信息
        List<TbMusicSingerPojo> singerIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(musicIds)) {
            singerIds = musicSingerService.list(Wrappers.<TbMusicSingerPojo>lambdaQuery().in(TbMusicSingerPojo::getMusicId, musicIds));
        }
        List<Long> singerLongIds = singerIds.stream().map(TbMusicSingerPojo::getSingerId).collect(Collectors.toList());
        Map<Long, TbSingerPojo> singerMap = new HashMap<>();
        if (CollUtil.isNotEmpty(singerLongIds)) {
            singerMap = singerService.listByIds(singerLongIds)
                                     .stream()
                                     .collect(Collectors.toMap(TbSingerPojo::getId, tbSingerPojo -> tbSingerPojo));
        }
        
        // 填充信息
        List<MusicPageRes> musicPageRes = new ArrayList<>();
        for (TbMusicPojo musicPojo : page.getRecords()) {
            MusicPageRes e = new MusicPageRes();
            e.setId(musicPojo.getId());
            e.setMusicName(musicPojo.getMusicName());
            e.setMusicNameAlias(musicPojo.getAliaName());
    
            // 专辑
            TbAlbumPojo tbAlbumPojo = Optional.ofNullable(albumMap.get(musicPojo.getAlbumId())).orElse(new TbAlbumPojo());
            e.setAlbumId(tbAlbumPojo.getId());
            e.setAlbumName(tbAlbumPojo.getAlbumName());
    
            // 歌手
            // 获取歌手ID
            List<Long> collect = singerIds.stream()
                                          .filter(tbMusicSingerPojo -> tbMusicSingerPojo.getMusicId().equals(musicPojo.getId()))
                                          .map(TbMusicSingerPojo::getSingerId)
                                          .collect(Collectors.toList());
            e.setSingerIds(new ArrayList<>());
            e.setSingerName(new ArrayList<>());
            for (Long aLong : collect) {
                TbSingerPojo tbSingerPojo = singerMap.get(aLong);
                e.getSingerIds().add(tbSingerPojo.getId());
                e.getSingerName().add(tbSingerPojo.getSingerName());
            }
            e.setTimeLength(musicPojo.getTimeLength());
            e.setCreateTime(musicPojo.getCreateTime());
            e.setOrder(req.getOrder());
            musicPageRes.add(e);
        }
        Page<MusicPageRes> pageVo = new Page<>();
        BeanUtils.copyProperties(page, pageVo);
        pageVo.setRecords(musicPageRes);
        return pageVo;
    }
    
    /**
     * 根据歌曲名获取歌曲ID
     *
     * @return 歌曲ID
     */
    private List<Long> getMusicIdByMusicName(MusicPageReq req) {
        if (StringUtils.isNotBlank(req.getMusicName())) {
            List<TbMusicPojo> list = new ArrayList<>();
            // 音乐名
            list.addAll(musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().like(TbMusicPojo::getMusicName, req.getMusicName())));
            // 别名
            list.addAll(musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().like(TbMusicPojo::getAliaName, req.getMusicName())));
            return list.stream().map(TbMusicPojo::getId).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    
    private List<Long> getMusicIDByAlbumName(MusicPageReq req) {
        if (StringUtils.isNotBlank(req.getAlbumName())) {
            // 获取专辑ID
            LambdaQueryWrapper<TbAlbumPojo> like = Wrappers.<TbAlbumPojo>lambdaQuery()
                                                           .like(TbAlbumPojo::getAlbumName, req.getAlbumName())
                                                           .like(TbAlbumPojo::getAlbumName, req.getAlbumName());
            List<TbAlbumPojo> albumList = albumService.list(like);
            List<Long> collect = albumList.stream().map(TbAlbumPojo::getId).collect(Collectors.toList());
            
            // 获取歌曲ID
            List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery()
                                                               .in(CollUtil.isNotEmpty(collect), TbMusicPojo::getAlbumId, collect));
            return list.stream().map(TbMusicPojo::getId).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    
    /**
     * 查询专辑名获取歌曲ID
     *
     * @return 歌曲ID
     */
    private List<Long> getMusicIDBySingerName(MusicPageReq req) {
        if (StringUtils.isNotBlank(req.getSingerName())) {
            LambdaQueryWrapper<TbSingerPojo> wrapper = Wrappers.lambdaQuery();
            List<TbSingerPojo> singerList = singerService.list(wrapper.like(TbSingerPojo::getSingerName, req.getSingerName()));
            List<Long> singerIdsList = singerList.stream().map(TbSingerPojo::getId).collect(Collectors.toList());
            if (IterUtil.isNotEmpty(singerIdsList)) {
                List<TbMusicSingerPojo> musicSingerList = musicSingerService.listByIds(singerIdsList);
                return musicSingerList.stream().map(TbMusicSingerPojo::getMusicId).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
