package org.api.admin.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.api.admin.config.AdminConfig;
import org.api.admin.model.req.AlbumReq;
import org.api.admin.model.req.SaveOrUpdateAlbumReq;
import org.api.admin.model.res.AlbumRes;
import org.api.admin.utils.MyPageUtil;
import org.api.common.service.QukuAPI;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.iservice.TbAlbumArtistService;
import org.core.iservice.TbAlbumService;
import org.core.iservice.TbArtistService;
import org.core.model.convert.AlbumConvert;
import org.core.model.convert.ArtistConvert;
import org.core.model.convert.MusicConvert;
import org.core.pojo.TbAlbumArtistPojo;
import org.core.pojo.TbAlbumPojo;
import org.core.pojo.TbArtistPojo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service(AdminConfig.ADMIN + "AlbumApi")
public class AlbumApi {
    
    /**
     * 专辑表
     */
    @Autowired
    private TbAlbumService albumService;
    
    /**
     * 专辑歌手中间表
     */
    @Autowired
    private TbAlbumArtistService albumSingerService;
    
    /**
     * 歌手表
     */
    @Autowired
    private TbArtistService singerService;
    
    @Autowired
    private QukuAPI qukuService;
    
    
    public Page<AlbumRes> getAllAlbumList(AlbumReq req) {
        req.setAlbumName(StringUtils.trim(req.getAlbumName()));
        req.setArtistName(StringUtils.trim(req.getArtistName()));
    
        req.setPage(MyPageUtil.checkPage(req.getPage()));
    
        List<TbAlbumPojo> albumList = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getAlbumName())) {
            LambdaQueryWrapper<TbAlbumPojo> albumWrapper = Wrappers.<TbAlbumPojo>lambdaQuery().like(TbAlbumPojo::getAlbumName, req.getAlbumName());
            albumList = albumService.list(albumWrapper);
        }
    
        List<Long> singerAlbumIdList = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getArtistName())) {
            LambdaQueryWrapper<TbArtistPojo> singerWrapper = Wrappers.<TbArtistPojo>lambdaQuery().like(TbArtistPojo::getArtistName, req.getArtistName());
            List<TbArtistPojo> singerList = singerService.list(singerWrapper);
            // 查询歌手表
            if (CollUtil.isNotEmpty(singerList)) {
                List<Long> collect = singerList.stream().map(TbArtistPojo::getId).collect(Collectors.toList());
                List<TbAlbumArtistPojo> list = albumSingerService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery()
                                                                               .in(TbAlbumArtistPojo::getArtistId, collect));
                singerAlbumIdList = list.stream().map(TbAlbumArtistPojo::getAlbumId).collect(Collectors.toList());
            }
        }
        List<Long> albumListId = albumList.stream().map(TbAlbumPojo::getId).collect(Collectors.toList());
        albumListId.addAll(singerAlbumIdList);
    
        LambdaQueryWrapper<TbAlbumPojo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        pageOrderBy(req.getOrder(), req.getOrderBy(), lambdaQueryWrapper);
        lambdaQueryWrapper.in(CollUtil.isNotEmpty(albumListId), TbAlbumPojo::getId, albumListId);
    
        // 查询全部专辑数据
        Page<TbAlbumPojo> albumPojoPage = new Page<>(req.getPage().getPageIndex(), req.getPage().getPageNum());
        Page<AlbumConvert> albumPage = qukuService.getAlbumPage(albumPojoPage, lambdaQueryWrapper);
    
        // 获取专辑ID，以供查询歌手信息
        Map<Long, List<ArtistConvert>> albumArtistMapByAlbumIds = new HashMap<>();
        if (CollUtil.isNotEmpty(albumPage.getRecords())) {
            List<Long> collect = albumPage.getRecords().stream().map(TbAlbumPojo::getId).collect(Collectors.toList());
            albumArtistMapByAlbumIds = qukuService.getAlbumArtistMapByAlbumIds(collect);
        }
    
        Page<AlbumRes> page = new Page<>();
        BeanUtils.copyProperties(albumPage, page, "records");
        page.setRecords(new ArrayList<>());
        for (AlbumConvert tbAlbumPojo : albumPage.getRecords()) {
            AlbumRes albumRes = new AlbumRes();
            albumRes.setArtistList(new ArrayList<>());
            BeanUtils.copyProperties(tbAlbumPojo, albumRes);
            albumRes.setPicUrl(tbAlbumPojo.getPicUrl());
    
            // 获取专辑中所有歌手
            List<ArtistConvert> artistConverts = albumArtistMapByAlbumIds.get(tbAlbumPojo.getId());
            albumRes.setArtistList(artistConverts);
    
            // 获取专辑下歌曲数量
            albumRes.setAlbumSize(qukuService.getAlbumMusicCountByAlbumId(tbAlbumPojo.getId()).longValue());
    
            albumRes.setOrderBy(req.getOrderBy());
            albumRes.setOrder(req.getOrder());
    
            page.getRecords().add(albumRes);
        }
    
        return page;
    }
    
    /**
     * 设置分页查询排序
     */
    private static void pageOrderBy(boolean order, String orderBy, LambdaQueryWrapper<TbAlbumPojo> musicWrapper) {
        // sort歌曲添加顺序, createTime创建日期顺序,updateTime修改日期顺序, id歌曲ID顺序
        switch (Optional.ofNullable(orderBy).orElse("")) {
            case "id":
                musicWrapper.orderBy(true, order, TbAlbumPojo::getId);
                break;
            case "updateTime":
                musicWrapper.orderBy(true, order, TbAlbumPojo::getUpdateTime);
                break;
            case "createTime":
            default:
                musicWrapper.orderBy(true, order, TbAlbumPojo::getCreateTime);
                break;
        }
    }
    
    
    /**
     * 添加音乐时选择专辑接口
     *
     * @param name 专辑名
     */
    public List<Map<String, Object>> getSelectAlbumList(String name) {
        LambdaQueryWrapper<TbAlbumPojo> desc = Wrappers.<TbAlbumPojo>lambdaQuery()
                                                       .like(StringUtils.isNotBlank(name), TbAlbumPojo::getAlbumName, name)
                                                       .orderByDesc(TbAlbumPojo::getUpdateTime);
        
        Page<TbAlbumPojo> page = albumService.page(new Page<>(0, 10), desc);
    
        ArrayList<Map<String, Object>> maps = new ArrayList<>();
        for (TbAlbumPojo albumPojo : page.getRecords()) {
            HashMap<String, Object> map = new HashMap<>();
            List<String> artistName = qukuService.getAlbumArtistListByAlbumIds(albumPojo.getId())
                                                 .parallelStream()
                                                 .map(TbArtistPojo::getArtistName)
                                                 .collect(Collectors.toList());
            String join = CollUtil.join(artistName, ",");
            String albumName = albumPojo.getAlbumName();
            String format = String.format("%s <b style='color: var(--el-color-primary);'>#%s#</b>", albumName, join);
            map.put("display", format);
            map.put("value", albumName);
            map.put("link", String.valueOf(albumPojo.getId()));
            map.putAll(BeanUtil.beanToMap(albumPojo));
            maps.add(map);
        }
        return maps;
    }
    
    public AlbumRes getAlbumInfo(Long albumId) {
        TbAlbumPojo byId = albumService.getById(albumId);
        Integer albumCount = qukuService.getAlbumMusicCountByAlbumId(albumId);
        List<MusicConvert> musicListByAlbumId = qukuService.getMusicListByAlbumId(albumId);
        List<ArtistConvert> artistListByAlbumIds = qukuService.getAlbumArtistListByAlbumIds(albumId);
    
        AlbumRes albumRes = new AlbumRes();
        albumRes.setArtistList(artistListByAlbumIds);
        albumRes.setMusicList(musicListByAlbumId);
        BeanUtils.copyProperties(byId, albumRes);
        albumRes.setPicUrl(qukuService.getPicUrl(byId.getId()));
        albumRes.setAlbumSize(Long.valueOf(albumCount));
        return albumRes;
    }
    
    public void deleteAlbum(List<Long> id, Boolean compel) {
        qukuService.deleteAlbum(id, compel);
    }
    
    public void saveOrUpdateAlbum(SaveOrUpdateAlbumReq req) {
        if (req.getId() == null && StringUtils.isBlank(req.getAlbumName())) {
            throw new BaseException(ResultCode.PARAM_NOT_COMPLETE);
        }
        albumService.saveOrUpdate(req);
        qukuService.saveOrUpdateAlbumPic(req.getId(), req.getPicConvert().getUrl());
    }
}
