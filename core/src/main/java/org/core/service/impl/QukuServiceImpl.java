package org.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.core.common.constant.PicTypeConstant;
import org.core.common.constant.TargetTagConstant;
import org.core.common.constant.defaultinfo.DefaultInfo;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.config.LyricConfig;
import org.core.config.PlayListTypeConfig;
import org.core.mybatis.iservice.*;
import org.core.mybatis.model.convert.AlbumConvert;
import org.core.mybatis.model.convert.ArtistConvert;
import org.core.mybatis.model.convert.CollectConvert;
import org.core.mybatis.model.convert.MusicConvert;
import org.core.mybatis.pojo.*;
import org.core.service.AccountService;
import org.core.service.QukuService;
import org.core.utils.CollectSortUtil;
import org.core.utils.ExceptionUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service("QukuService")
@Slf4j
public class QukuServiceImpl implements QukuService {
    private static final Object picLock = new Object();
    public static final Object addLabelLock = new Object();
    public static final Object removeLabelLock = new Object();
    
    private final TbMusicService musicService;
    
    private final TbAlbumService albumService;
    
    private final TbArtistService artistService;
    
    private final TbResourceService musicUrlService;
    
    private final TbUserAlbumService userAlbumService;
    
    private final TbAlbumArtistService albumArtistService;
    
    private final TbMusicArtistService musicArtistService;
    
    private final TbUserArtistService userSingerService;
    
    private final TbCollectMusicService collectMusicService;
    
    private final TbCollectService collectService;
    
    private final TbUserCollectService userCollectService;
    
    private final TbMiddleTagService middleTagService;
    
    private final TbLyricService lyricService;
    
    private final TbTagService tagService;
    
    private final AccountService accountService;
    
    private final TbPicService picService;
    
    private final TbMiddlePicService middlePicService;
    
    private final Cache<Long, TbPicPojo> picCache;
    
    private final Cache<Long, Long> picMiddleCache;
    
    private final DefaultInfo defaultInfo;
    
    
    private static List<AlbumConvert> getAlbumConvertList(List<TbAlbumPojo> albumPojoList, Map<Long, String> picUrl) {
        return albumPojoList.parallelStream().map(tbAlbumPojo -> {
            AlbumConvert convert = new AlbumConvert();
            BeanUtils.copyProperties(tbAlbumPojo, convert);
            convert.setPicUrl(MapUtil.get(picUrl, tbAlbumPojo.getId(), String.class));
            return convert;
        }).toList();
    }
    
    private static Map<Long, ArtistConvert> getLongArtistConvertMap(List<TbArtistPojo> tbArtistPojos, Map<Long, String> picUrl) {
        return tbArtistPojos.parallelStream().collect(Collectors.toMap(TbArtistPojo::getId, tbArtistPojo -> {
            ArtistConvert convert = new ArtistConvert();
            BeanUtils.copyProperties(tbArtistPojo, convert);
            convert.setPicUrl(MapUtil.get(picUrl, tbArtistPojo.getId(), String.class));
            return convert;
        }));
    }
    
    private static List<ArtistConvert> getArtistConvertList(List<TbArtistPojo> tbArtistPojos, Map<Long, String> picUrl) {
        return tbArtistPojos.parallelStream().map(tbArtistPojo -> {
            ArtistConvert convert = new ArtistConvert();
            BeanUtils.copyProperties(tbArtistPojo, convert);
            convert.setPicUrl(MapUtil.get(picUrl, tbArtistPojo.getId(), String.class));
            return convert;
        }).toList();
    }
    
    private static List<MusicConvert> getMusicConvertList(List<TbMusicPojo> tbMusicPojos, Map<Long, String> picUrl) {
        return tbMusicPojos.parallelStream().map(tbArtistPojo -> {
            MusicConvert convert = new MusicConvert();
            BeanUtils.copyProperties(tbArtistPojo, convert);
            convert.setPicUrl(MapUtil.get(picUrl, tbArtistPojo.getId(), String.class));
            return convert;
        }).toList();
    }
    
    private static List<CollectConvert> getCollectConvertList(List<TbCollectPojo> collectConverts, Map<Long, String> picUrl) {
        return collectConverts.parallelStream().map(tbArtistPojo -> {
            CollectConvert convert = new CollectConvert();
            BeanUtils.copyProperties(tbArtistPojo, convert);
            convert.setPicUrl(MapUtil.get(picUrl, tbArtistPojo.getId(), String.class));
            return convert;
        }).toList();
    }
    
    /**
     * 获取专辑信息
     */
    @Override
    public AlbumConvert getAlbumByMusicId(Long musicId) {
        List<AlbumConvert> albumListByMusicId = getAlbumListByMusicId(Collections.singletonList(musicId));
        if (CollUtil.isEmpty(albumListByMusicId)) {
            return null;
        }
        if (albumListByMusicId.size() != 1) {
            throw new BaseException(ResultCode.ALBUM_ERROR);
        }
        return albumListByMusicId.get(0);
    }
    
    @Override
    public AlbumConvert getAlbumByAlbumId(Long albumIds) {
        List<AlbumConvert> albumListByAlbumId = getAlbumListByAlbumId(Collections.singletonList(albumIds));
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
    public List<AlbumConvert> getAlbumListByMusicId(List<Long> musicIds) {
        if (CollUtil.isEmpty(musicIds)) {
            return Collections.emptyList();
        }
        List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getId, musicIds));
        List<Long> albumIds = list.stream().map(TbMusicPojo::getAlbumId).toList();
        return getAlbumListByAlbumId(albumIds);
    }
    
    /**
     * 通过专辑ID 获取专辑信息
     */
    @Override
    public List<AlbumConvert> getAlbumListByAlbumId(Collection<Long> albumIds) {
        List<TbAlbumPojo> albumPojoList = albumService.listByIds(albumIds);
        List<Long> collect = albumPojoList.parallelStream().map(TbAlbumPojo::getId).toList();
        Map<Long, String> picUrl = getCollectPicUrl(collect);
        return getAlbumConvertList(albumPojoList, picUrl);
    }
    
    /**
     * 查询数据歌曲下载地址
     * key music value url
     *
     * @param musicId 音乐ID
     */
    @Override
    public Map<Long, List<TbResourcePojo>> getMusicMapUrl(Collection<Long> musicId) {
        LambdaQueryWrapper<TbResourcePojo> in = Wrappers.<TbResourcePojo>lambdaQuery().in(TbResourcePojo::getMusicId, musicId);
        List<TbResourcePojo> list = musicUrlService.list(in);
        return list.parallelStream()
                   .collect(Collectors.toConcurrentMap(TbResourcePojo::getMusicId, ListUtil::toList, (objects, objects2) -> {
                       objects2.addAll(objects);
                       return objects2;
                   }));
    }
    
    /**
     * 批量获取歌手信息
     * Long -> music ID
     */
    @Override
    public List<ArtistConvert> getAlbumArtistListByMusicId(List<Long> musicIds) {
        List<TbMusicPojo> musicPojoList = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getId, musicIds));
        if (CollUtil.isEmpty(musicPojoList)) {
            return Collections.emptyList();
        }
        Set<Long> albumIds = musicPojoList.stream().map(TbMusicPojo::getAlbumId).collect(Collectors.toSet());
        List<TbAlbumArtistPojo> list = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getAlbumId, albumIds));
        return getTbSingerPojoList(CollUtil.isEmpty(list), list.stream().map(TbAlbumArtistPojo::getArtistId));
    }
    
    /**
     * 批量获取歌手信息，优化版本
     * key 值为音乐ID
     * value 为歌手
     *
     * @param albumPojoMap key Music, value ID 专辑信息
     */
    @Override
    public Map<Long, List<ArtistConvert>> getAlbumArtistListByMusicIdToMap(Map<Long, TbAlbumPojo> albumPojoMap) {
        Set<Long> collect = albumPojoMap.values().parallelStream().map(TbAlbumPojo::getId).collect(Collectors.toSet());
        if (CollUtil.isEmpty(collect)) {
            return MapUtil.empty();
        }
        List<TbAlbumArtistPojo> list = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getAlbumId, collect));
        
        Map<Long, List<TbAlbumArtistPojo>> albumArtistMap = new HashMap<>();
        for (TbAlbumArtistPojo tbAlbumArtistPojo : list) {
            List<TbAlbumArtistPojo> pojos = albumArtistMap.get(tbAlbumArtistPojo.getAlbumId());
            if (pojos == null) {
                ArrayList<TbAlbumArtistPojo> value = new ArrayList<>();
                value.add(tbAlbumArtistPojo);
                albumArtistMap.put(tbAlbumArtistPojo.getAlbumId(), value);
            } else {
                pojos.add(tbAlbumArtistPojo);
                albumArtistMap.put(tbAlbumArtistPojo.getAlbumId(), pojos);
            }
        }
        
        Set<Long> artistIds = list.parallelStream().map(TbAlbumArtistPojo::getArtistId).collect(Collectors.toSet());
        List<TbArtistPojo> artistPojoList = artistService.listByIds(artistIds);
        
        Map<Long, String> picUrl = getArtistPicUrl(artistIds);
        Map<Long, ArtistConvert> artistMap = getLongArtistConvertMap(artistPojoList, picUrl);
        
        Map<Long, List<ArtistConvert>> resMap = new HashMap<>();
        for (Map.Entry<Long, TbAlbumPojo> longTbAlbumPojoEntry : albumPojoMap.entrySet()) {
            TbAlbumPojo tbAlbumPojo = longTbAlbumPojoEntry.getValue();
            List<TbAlbumArtistPojo> tbAlbumArtistPojos = albumArtistMap.get(tbAlbumPojo.getId()) == null
                    ? new ArrayList<>()
                    : albumArtistMap.get(tbAlbumPojo.getId());
            List<ArtistConvert> tbArtistPojos = tbAlbumArtistPojos.parallelStream()
                                                                  .map(tbAlbumArtistPojo -> artistMap.get(tbAlbumArtistPojo.getArtistId()))
                                                                  .toList();
            resMap.put(longTbAlbumPojoEntry.getKey(), tbArtistPojos);
        }
        return resMap;
    }
    
    /**
     * 获取歌手信息
     */
    @Override
    public List<ArtistConvert> getAlbumArtistByMusicId(Long musicId) {
        return getAlbumArtistListByMusicId(Collections.singletonList(musicId));
    }
    
    @Override
    public List<TbResourcePojo> getMusicPaths(Collection<Long> musicId) {
        LambdaQueryWrapper<TbResourcePojo> in = Wrappers.<TbResourcePojo>lambdaQuery().in(TbResourcePojo::getMusicId, musicId);
        return musicUrlService.list(in);
    }
    
    /**
     * 查询专辑下音乐数量
     *
     * @param albumId 专辑ID
     */
    @Override
    public Integer getAlbumMusicCountByAlbumId(Long albumId) {
        long count = musicService.count(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getAlbumId, albumId));
        return Math.toIntExact(count);
    }
    
    /**
     * 查询专辑下音乐数量
     *
     * @param musicId 歌曲ID
     */
    @Override
    public Integer getAlbumMusicCountByMusicId(Long musicId) {
        long count = musicService.count(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getId, musicId));
        return Math.toIntExact(count);
    }
    
    /**
     * 通过歌手获取歌手拥有的音乐数量
     *
     * @param id 歌手ID
     */
    @Override
    public Long getMusicCountBySingerId(Long id) {
        List<TbAlbumArtistPojo> albumSingerPojoList = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery()
                                                                                      .eq(TbAlbumArtistPojo::getArtistId, id));
        if (CollUtil.isEmpty(albumSingerPojoList)) {
            return 0L;
        }
        Set<Long> albumIds = albumSingerPojoList.stream().map(TbAlbumArtistPojo::getAlbumId).collect(Collectors.toSet());
        return musicService.count(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getAlbumId, albumIds));
    }
    
    /**
     * 随即获取曲库中的一条数据
     */
    @Override
    public MusicConvert randomMusic() {
        long count = musicService.count();
        Page<TbMusicPojo> page = new Page<>(RandomUtil.randomLong(0, count), 1);
        musicService.page(page);
        TbMusicPojo musicPojo = Optional.ofNullable(page.getRecords()).orElse(new ArrayList<>()).get(0);
        MusicConvert convert = new MusicConvert();
        BeanUtils.copyProperties(musicPojo, convert);
        convert.setPicUrl(getMusicPicUrl(musicPojo.getId()));
        return convert;
    }
    
    @Override
    public Page<AlbumConvert> getRandomAlbum(String area, Long offset, Long limit) {
        Page<TbAlbumPojo> page = new Page<>(offset, limit);
        albumService.page(page);
        return getAlbumConvertPage(page);
    }
    
    /**
     * @param albumPojoPage      专辑分页参数
     * @param lambdaQueryWrapper 查询参数
     * @return 返回数据
     */
    @Override
    public Page<AlbumConvert> getAlbumPage(Page<TbAlbumPojo> albumPojoPage, Wrapper<TbAlbumPojo> lambdaQueryWrapper) {
        albumService.page(albumPojoPage, lambdaQueryWrapper);
        return getAlbumConvertPage(albumPojoPage);
    }
    
    @NotNull
    private Page<AlbumConvert> getAlbumConvertPage(Page<TbAlbumPojo> albumPojoPage) {
        Map<Long, String> picUrl = getAlbumPicUrl(albumPojoPage.getRecords().parallelStream().map(TbAlbumPojo::getId).toList());
        Page<AlbumConvert> convertPage = new Page<>();
        BeanUtils.copyProperties(albumPojoPage, convertPage);
        convertPage.setRecords(getAlbumConvertList(albumPojoPage.getRecords(), picUrl));
        return convertPage;
    }
    
    /**
     * 获取专辑歌手列表
     */
    @Override
    public List<ArtistConvert> getAlbumArtistListByAlbumIds(Long albumIds) {
        return getAlbumArtistListByAlbumIds(Collections.singletonList(albumIds));
    }
    
    @Override
    public List<AlbumConvert> getAlbumListByArtistIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TbAlbumArtistPojo> in = Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getArtistId, ids);
        List<TbAlbumArtistPojo> list = albumArtistService.list(in);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return getAlbumListByAlbumId(list.stream().map(TbAlbumArtistPojo::getAlbumId).collect(Collectors.toSet()));
    }
    
    /**
     * 通过专辑ID获取歌手列表
     */
    @Override
    public List<ArtistConvert> getAlbumArtistListByAlbumIds(List<Long> albumIds) {
        List<TbAlbumArtistPojo> list = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getAlbumId, albumIds));
        return getTbSingerPojoList(CollUtil.isEmpty(list), list.stream().map(TbAlbumArtistPojo::getArtistId));
    }
    
    /**
     * 获取专辑歌手列表
     * Map
     * key to Album ID
     * value to Artist List
     *
     * @param albumIds 专辑ID
     */
    @Override
    public Map<Long, List<ArtistConvert>> getAlbumArtistMapByAlbumIds(Collection<Long> albumIds) {
        List<TbAlbumArtistPojo> list = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getAlbumId, albumIds));
        if (CollUtil.isEmpty(list)) {
            return new HashMap<>();
        }
        List<Long> artists = list.parallelStream().map(TbAlbumArtistPojo::getArtistId).toList();
        List<TbArtistPojo> tbArtistPojos = artistService.list(Wrappers.<TbArtistPojo>lambdaQuery().in(TbArtistPojo::getId, artists));
        Map<Long, String> picUrl = getArtistPicUrl(artists);
        Map<Long, ArtistConvert> collect = getLongArtistConvertMap(tbArtistPojos, picUrl);
        return list.parallelStream()
                   .collect(Collectors.toMap(TbAlbumArtistPojo::getAlbumId,
                           tbAlbumArtistPojo -> ListUtil.toList(collect.get(tbAlbumArtistPojo.getArtistId())),
                           (tbArtistPojo, tbArtistPojo2) -> {
                               tbArtistPojo2.addAll(tbArtistPojo);
                               return tbArtistPojo2;
                           }));
    }
    
    /**
     * 获取歌曲歌手列表
     *
     * @param musicId 歌手ID
     * @return 歌手列表
     */
    @Override
    public List<ArtistConvert> getMusicArtistByMusicId(Collection<Long> musicId) {
        if (CollUtil.isEmpty(musicId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TbMusicArtistPojo> eq = Wrappers.<TbMusicArtistPojo>lambdaQuery().in(TbMusicArtistPojo::getMusicId, musicId);
        List<TbMusicArtistPojo> list = musicArtistService.list(eq);
        Collection<Long> collect = list.parallelStream().map(TbMusicArtistPojo::getArtistId).collect(Collectors.toSet());
        if (CollUtil.isEmpty(collect)) {
            return Collections.emptyList();
        }
        List<TbArtistPojo> tbArtistPojos = artistService.listByIds(collect);
        return getArtistConvertList(tbArtistPojos, getCollectPicUrl(collect));
    }
    
    /**
     * 获取歌曲歌手列表
     *
     * @param musicId 歌手ID
     * @return 歌手列表
     */
    @Override
    public Map<Long, List<ArtistConvert>> getMusicArtistByMusicIdToMap(Collection<Long> musicId) {
        if (CollUtil.isEmpty(musicId)) {
            return Collections.emptyMap();
        }
        List<TbMusicArtistPojo> list = musicArtistService.list(Wrappers.<TbMusicArtistPojo>lambdaQuery().in(TbMusicArtistPojo::getMusicId, musicId));
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        List<Long> artistIds = list.parallelStream().map(TbMusicArtistPojo::getArtistId).toList();
        List<TbArtistPojo> tbArtistPojos = artistService.listByIds(artistIds);
        Map<Long, ArtistConvert> artistMap = getLongArtistConvertMap(tbArtistPojos, getArtistPicUrl(artistIds));
        
        HashMap<Long, List<ArtistConvert>> longListHashMap = new HashMap<>();
        for (TbMusicArtistPojo tbMusicArtistPojo : list) {
            Long artistId = tbMusicArtistPojo.getArtistId();
            ArtistConvert tbArtistPojo = artistMap.get(artistId);
            List<ArtistConvert> tbArtistPojos1 = longListHashMap.get(tbMusicArtistPojo.getMusicId());
            if (CollUtil.isEmpty(tbArtistPojos1)) {
                longListHashMap.put(tbMusicArtistPojo.getMusicId(), CollUtil.newArrayList(tbArtistPojo));
            } else {
                tbArtistPojos1.add(tbArtistPojo);
                longListHashMap.put(tbMusicArtistPojo.getMusicId(), tbArtistPojos1);
            }
        }
        
        return longListHashMap;
    }
    
    /**
     * 通过专辑ID获取歌手
     *
     * @param empty      是否执行
     * @param longStream 专辑ID流
     */
    private List<ArtistConvert> getTbSingerPojoList(boolean empty, Stream<Long> longStream) {
        if (empty) {
            return Collections.emptyList();
        }
        List<Long> artistIds = longStream.toList();
        List<TbArtistPojo> list = artistService.list(Wrappers.<TbArtistPojo>lambdaQuery().in(TbArtistPojo::getId, artistIds));
        Map<Long, String> picUrl = getArtistPicUrl(artistIds);
        return getArtistConvertList(list, picUrl);
    }
    
    /**
     * 获取歌手所有专辑数量
     *
     * @param id 歌手ID
     */
    @Override
    public Integer getAlbumCountBySingerId(Long id) {
        return Math.toIntExact(albumArtistService.count(Wrappers.<TbAlbumArtistPojo>lambdaQuery().eq(TbAlbumArtistPojo::getArtistId, id)));
    }
    
    /**
     * 查询用户收藏专辑
     *
     * @param user    用户数据
     * @param current 当前页数
     * @param size    每页多少数据
     */
    @Override
    public List<AlbumConvert> getUserCollectAlbum(SysUserPojo user, Long current, Long size) {
        List<TbUserAlbumPojo> userAlbumPojoList = userAlbumService.list(Wrappers.<TbUserAlbumPojo>lambdaQuery()
                                                                                .eq(TbUserAlbumPojo::getUserId, user.getId()));
        if (CollUtil.isEmpty(userAlbumPojoList)) {
            return Collections.emptyList();
        }
        List<Long> albumIds = userAlbumPojoList.stream().map(TbUserAlbumPojo::getAlbumId).toList();
        return getAlbumListByAlbumId(albumIds);
    }
    
    /**
     * 获取用户关注歌手
     *
     * @param user 用户信息
     */
    @Override
    public List<ArtistConvert> getUserLikeSingerList(SysUserPojo user) {
        List<TbUserArtistPojo> userLikeSinger = userSingerService.list(Wrappers.<TbUserArtistPojo>lambdaQuery()
                                                                               .eq(TbUserArtistPojo::getUserId, user.getId()));
        if (CollUtil.isEmpty(userLikeSinger)) {
            return Collections.emptyList();
        }
        List<Long> aritstIds = userLikeSinger.stream().map(TbUserArtistPojo::getArtistId).toList();
        return getArtistConvertList(artistService.listByIds(aritstIds), getArtistPicUrl(aritstIds));
    }
    
    /**
     * 根据专辑ID查找音乐
     *
     * @param id 专辑ID
     */
    @Override
    public List<MusicConvert> getMusicListByAlbumId(Long id) {
        List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getAlbumId, id));
        return getMusicConvertList(list, getMusicPicUrl(list.parallelStream().map(TbMusicPojo::getId).toList()));
    }
    
    @Override
    public List<MusicConvert> getMusicListByAlbumId(Collection<Long> ids) {
        List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getAlbumId, ids));
        return getMusicConvertList(list, getMusicPicUrl(list.parallelStream().map(TbMusicPojo::getId).toList()));
    }
    
    /**
     * 根据歌手名查找音乐
     *
     * @param name 歌手
     */
    @Override
    public List<MusicConvert> getMusicListByArtistName(String name) {
        if (StringUtils.isNotBlank(name)) {
            List<TbArtistPojo> singerList = artistService.list(Wrappers.<TbArtistPojo>lambdaQuery().like(TbArtistPojo::getArtistName, name));
            List<Long> singerIdsList = singerList.stream().map(TbArtistPojo::getId).toList();
            if (CollUtil.isEmpty(singerList)) {
                return Collections.emptyList();
            }
            List<TbAlbumArtistPojo> albumIds = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery()
                                                                               .in(TbAlbumArtistPojo::getArtistId, singerIdsList));
            if (IterUtil.isNotEmpty(albumIds)) {
                return getMusicListByAlbumId(albumIds.stream().map(TbAlbumArtistPojo::getAlbumId).collect(Collectors.toSet()));
            }
        }
        return Collections.emptyList();
    }
    
    /**
     * 获取歌手下音乐信息
     *
     * @param id 歌手ID
     */
    @Override
    public List<MusicConvert> getMusicListByArtistId(Long id) {
        List<TbMusicArtistPojo> musicArtistPojos = musicArtistService.list(Wrappers.<TbMusicArtistPojo>lambdaQuery()
                                                                                   .eq(TbMusicArtistPojo::getArtistId, id));
        if (CollUtil.isEmpty(musicArtistPojos)) {
            return Collections.emptyList();
        }
        List<Long> collect = musicArtistPojos.parallelStream().map(TbMusicArtistPojo::getMusicId).toList();
        
        List<TbMusicPojo> list = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getId, collect));
        return getMusicConvertList(list, getMusicPicUrl(list.parallelStream().map(TbMusicPojo::getId).toList()));
    }
    
    /**
     * 随机获取歌手
     *
     * @param count 获取数量
     */
    @Override
    public List<ArtistConvert> randomSinger(int count) {
        long sum = artistService.count();
        ArrayList<TbArtistPojo> res = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long randomNum = RandomUtil.randomLong(sum);
            Page<TbArtistPojo> page = artistService.page(new Page<>(randomNum, 1));
            res.addAll(page.getRecords());
        }
        return getArtistConvertList(res, getArtistPicUrl(res.parallelStream().map(TbArtistPojo::getId).toList()));
    }
    
    /**
     * 添加音乐到歌单
     *
     * @param userID    用户ID
     * @param collectId 歌单数据
     * @param songIds   歌曲列表
     * @param flag      删除还是添加
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMusicToCollect(Long userID, Long collectId, List<Long> songIds, boolean flag) {
        if (flag) {
            // 查询歌单内歌曲是否存在
            long count = collectMusicService.count(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                           .eq(TbCollectMusicPojo::getCollectId, collectId)
                                                           .in(TbCollectMusicPojo::getMusicId, songIds));
            ExceptionUtil.isNull(count > 0, ResultCode.PLAT_LIST_MUSIC_EXIST);
            
            // 添加
            List<TbMusicPojo> tbMusicPojo = musicService.listByIds(songIds);
            
            long allCount = getCollectCount(collectId);
            List<TbCollectMusicPojo> collect = new ArrayList<>(tbMusicPojo.size());
            for (TbMusicPojo musicPojo : tbMusicPojo) {
                TbCollectMusicPojo tbCollectMusicPojo = new TbCollectMusicPojo();
                tbCollectMusicPojo.setCollectId(collectId);
                tbCollectMusicPojo.setMusicId(musicPojo.getId());
                allCount++;
                tbCollectMusicPojo.setSort(allCount);
                collect.add(tbCollectMusicPojo);
            }
            collectMusicService.saveBatch(collect);
            log.info("add collect music: {}, music list{}", collectId, CollUtil.join(songIds, ","));
            // 更新封面
            Long songId = songIds.get(songIds.size() - 1);
            TbMusicPojo musicPojo = musicService.getById(songId);
            TbCollectPojo entity = new TbCollectPojo();
            entity.setId(collectId);
            if (musicPojo != null) {
                String picUrl = getMusicPicUrl(songId);
                this.saveOrUpdateCollectPic(entity.getId(), picUrl);
            }
            collectService.updateById(entity);
        } else {
            // 删除歌曲
            collectMusicService.remove(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                               .eq(TbCollectMusicPojo::getCollectId, collectId)
                                               .in(TbCollectMusicPojo::getMusicId, songIds));
            log.debug("remove playlist: {}, music list: {}", collectId, CollUtil.join(songIds, ","));
        }
    }
    
    /**
     * 检查用户是否有权限操作歌单
     *
     * @param userId        用户ID
     * @param tbCollectPojo 歌单信息
     */
    public static void checkUserAuth(Long userId, TbCollectPojo tbCollectPojo) {
        // 检查是否有该歌单
        if (tbCollectPojo == null || tbCollectPojo.getUserId() == null) {
            throw new BaseException(ResultCode.SONG_LIST_DOES_NOT_EXIST);
        }
        // 检查用户是否有权限
        if (!userId.equals(tbCollectPojo.getUserId())) {
            log.warn(ResultCode.PERMISSION_NO_ACCESS.getResultMsg());
            throw new BaseException(ResultCode.PERMISSION_NO_ACCESS);
        }
    }
    
    /**
     * 添加歌单
     *
     * @param userId 用户ID
     * @param name   歌单名
     * @param type   歌单类型，0为普通歌单，1为用户喜爱歌单，2为推荐歌单
     * @return 歌单创建信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectConvert createPlayList(Long userId, String name, byte type) {
        TbCollectPojo collectPojo = new TbCollectPojo();
        synchronized (picLock) {
            // 歌单表保存
            collectPojo.setUserId(userId);
            collectPojo.setPlayListName(name);
            collectPojo.setSort(collectService.count() + 1);
            collectPojo.setType(type);
            collectService.save(collectPojo);
        }
        // 保存用户关联表
        TbUserCollectPojo entity = new TbUserCollectPojo();
        entity.setCollectId(collectPojo.getId());
        entity.setUserId(userId);
        userCollectService.save(entity);
        
        TbPicPojo pic = new TbPicPojo();
        pic.setUrl(defaultInfo.getPic().getPlayListPic());
        this.saveOrUpdateCollectPic(collectPojo.getId(), defaultInfo.getPic().getPlayListPic());
        
        // 封面查询
        CollectConvert convert = new CollectConvert();
        BeanUtils.copyProperties(collectPojo, convert);
        convert.setPicUrl(getCollectPicUrl(entity.getCollectId()));
        return convert;
    }
    
    /**
     * 删除歌单
     *
     * @param userId      用户ID
     * @param collectList 删除歌单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePlayList(Long userId, Collection<Long> collectList) {
        if (CollUtil.isEmpty(collectList)) {
            return;
        }
        List<TbCollectPojo> tbCollectPojos = collectService.listByIds(collectList);
        for (TbCollectPojo tbCollectPojo : tbCollectPojos) {
            checkUserAuth(userId, tbCollectPojo);
        }
        // 删除歌单ID
        collectService.removeByIds(collectList);
        // 删除封面
        removePicIds(new ArrayList<>(collectList));
        // 删除歌单关联ID
        collectMusicService.remove(Wrappers.<TbCollectMusicPojo>lambdaQuery().in(TbCollectMusicPojo::getCollectId, collectList));
        // 删除歌单关联tag
        middleTagService.remove(Wrappers.<TbMiddleTagPojo>lambdaQuery()
                                        .eq(TbMiddleTagPojo::getType, TargetTagConstant.TARGET_COLLECT_TAG)
                                        .in(TbMiddleTagPojo::getMiddleId, collectList));
    }
    
    /**
     * 获取用户所有音乐，包括喜爱歌单
     *
     * @param uid  用户ID
     * @param type 歌单类型
     * @return 返回用户创建歌单
     */
    @Override
    public List<CollectConvert> getUserPlayList(Long uid, Collection<Byte> type) {
        LambdaQueryWrapper<TbCollectPojo> queryWrapper = Wrappers.<TbCollectPojo>lambdaQuery()
                                                                 .eq(TbCollectPojo::getUserId, uid)
                                                                 .in(CollUtil.isNotEmpty(type), TbCollectPojo::getType, type);
        List<TbCollectPojo> list = collectService.list(queryWrapper);
        List<TbCollectPojo> collectPojoList = CollectSortUtil.userLikeUserSort(uid, list);
        
        if (CollUtil.isEmpty(collectPojoList)) {
            return Collections.emptyList();
        }
        return getCollectConvertList(collectPojoList,
                getCollectPicUrl(collectPojoList.parallelStream().map(TbCollectPojo::getId).toList()));
    }
    
    /**
     * 获取歌单音乐数量
     *
     * @param id 歌单ID
     * @return Long 歌单ID Integer 音乐数量
     */
    @Override
    public Integer getCollectMusicCount(Long id) {
        long count = collectMusicService.count(Wrappers.<TbCollectMusicPojo>lambdaQuery().eq(TbCollectMusicPojo::getCollectId, id));
        return Math.toIntExact(count);
    }
    
    /**
     * 获取歌曲歌词
     *
     * @param musicId 歌词ID
     * @return 歌词列表
     */
    @Override
    public List<TbLyricPojo> getMusicLyric(Long musicId) {
        return lyricService.list(Wrappers.<TbLyricPojo>lambdaQuery().eq(TbLyricPojo::getMusicId, musicId));
    }
    
    /**
     * 对歌单tag，音乐添加tag， 或者指定音乐流派
     *
     * @param target 指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id     歌单或歌曲前ID
     * @param label  标签名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLabel(Byte target, Long id, String label) {
        TbTagPojo tagPojo = tagService.getOne(Wrappers.<TbTagPojo>lambdaQuery().eq(TbTagPojo::getTagName, label));
        Optional<TbTagPojo> pojo = Optional.ofNullable(tagPojo);
        tagPojo = pojo.orElseGet(() -> {
            TbTagPojo entity = new TbTagPojo();
            entity.setTagName(label);
            tagService.saveOrUpdate(entity);
            return entity;
        });
        addLabel(target, id, tagPojo.getId());
    }
    
    /**
     * 批量添加tag
     *
     * @param target 指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id     歌单或歌曲前ID
     * @param labels 标签名
     */
    @Override
    public void addLabel(Byte target, Long id, List<String> labels) {
        // 新增tag
        List<TbTagPojo> list;
        synchronized (addLabelLock) {
            list = tagService.list(Wrappers.<TbTagPojo>lambdaQuery().eq(TbTagPojo::getTagName, labels));
            Map<String, TbTagPojo> collect = list.parallelStream().collect(Collectors.toMap(TbTagPojo::getTagName, tbTagPojo -> tbTagPojo));
            for (String label : labels) {
                if (collect.get(label) == null) {
                    TbTagPojo entity = new TbTagPojo();
                    entity.setTagName(label);
                    list.add(entity);
                }
            }
            tagService.saveOrUpdateBatch(list);
        }
        Set<Long> tagIds = list.parallelStream().map(TbTagPojo::getId).collect(Collectors.toSet());
        // 关联到对应ID
        addLabel(target, id, tagIds);
    }
    
    /**
     * 添加歌单，音乐tag， 或者指定音乐流派
     *
     * @param target  指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id      歌单, 专辑或歌曲ID
     * @param labelId 标签ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLabel(Byte target, Long id, Long labelId) {
        // 添加标签
        LambdaQueryWrapper<TbMiddleTagPojo> eq = Wrappers.<TbMiddleTagPojo>lambdaQuery()
                                                         .eq(TbMiddleTagPojo::getType, target)
                                                         .eq(TbMiddleTagPojo::getTagId, labelId)
                                                         .eq(TbMiddleTagPojo::getMiddleId, id);
        TbMiddleTagPojo one = middleTagService.getOne(eq);
        one = Optional.ofNullable(one).orElse(new TbMiddleTagPojo());
        one.setMiddleId(id);
        one.setTagId(labelId);
        one.setType(target);
        middleTagService.save(one);
    }
    
    /**
     * 批量添加tag
     *
     * @param target 指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id     歌单或歌曲前ID
     * @param tagIds 标签ID
     */
    @Override
    public void addLabel(Byte target, Long id, Set<Long> tagIds) {
        LambdaQueryWrapper<TbMiddleTagPojo> in = Wrappers.<TbMiddleTagPojo>lambdaQuery().in(TbMiddleTagPojo::getTagId, tagIds);
        List<TbMiddleTagPojo> list = middleTagService.list(in);
        Map<Long, TbMiddleTagPojo> collect = list.parallelStream()
                                                 .collect(Collectors.toMap(TbMiddleTagPojo::getTagId, tbMiddleTagPojo -> tbMiddleTagPojo));
        List<TbMiddleTagPojo> middleTagPojos = new ArrayList<>();
        for (Long labelId : tagIds) {
            if (collect.get(labelId) == null) {
                TbMiddleTagPojo entity = new TbMiddleTagPojo();
                entity.setTagId(labelId);
                entity.setMiddleId(id);
                entity.setType(target);
                middleTagPojos.add(entity);
            }
        }
        middleTagService.saveOrUpdateBatch(middleTagPojos);
    }
    
    /**
     * 删除全部tag
     *
     * @param id 音乐，歌单， 专辑
     */
    @Override
    public void removeLabelAll(Long id) {
        synchronized (removeLabelLock) {
            LambdaQueryWrapper<TbMiddleTagPojo> eq = Wrappers.<TbMiddleTagPojo>lambdaQuery().eq(TbMiddleTagPojo::getMiddleId, id);
            List<TbMiddleTagPojo> tagPojoList = middleTagService.list(eq);
            if (CollUtil.isEmpty(tagPojoList)) {
                return;
            }
            Map<Long, ArrayList<TbMiddleTagPojo>> arrayListMap = tagPojoList.parallelStream()
                                                                            .collect(Collectors.toMap(TbMiddleTagPojo::getId,
                                                                                    ListUtil::toList,
                                                                                    (objects, objects2) -> {
                                                                                        objects2.addAll(objects);
                                                                                        return objects2;
                                                                                    }));
            ArrayList<Long> tagIds = new ArrayList<>();
            for (Map.Entry<Long, ArrayList<TbMiddleTagPojo>> longArrayListEntry : arrayListMap.entrySet()) {
                // 无关联的tag直接删除
                if (longArrayListEntry.getValue().size() == 1) {
                    tagIds.add(longArrayListEntry.getValue().get(0).getTagId());
                }
            }
            middleTagService.remove(eq);
            tagService.removeByIds(tagIds);
        }
        
    }
    
    /**
     * 删除歌单或音乐中的tag
     *
     * @param target       指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id           歌单或歌曲前ID
     * @param labelBatchId 需要删除的label ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLabelById(Byte target, Long id, Collection<Long> labelBatchId) {
        LambdaQueryWrapper<TbMiddleTagPojo> eq = Wrappers.<TbMiddleTagPojo>lambdaQuery()
                                                         .eq(TbMiddleTagPojo::getType, target)
                                                         .eq(TbMiddleTagPojo::getMiddleId, id)
                                                         .in(TbMiddleTagPojo::getTagId, labelBatchId);
        middleTagService.remove(eq);
    }
    
    /**
     * 删除歌单或音乐中的tag
     *
     * @param target         指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id             歌单或歌曲前ID
     * @param labelBatchName 需要删除的label ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLabelByName(Byte target, Long id, Collection<Long> labelBatchName) {
        LambdaQueryWrapper<TbTagPojo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(TbTagPojo::getTagName, labelBatchName);
        List<TbTagPojo> list = tagService.list(queryWrapper);
        Set<Long> collect = list.parallelStream().map(TbTagPojo::getId).collect(Collectors.toSet());
        removeLabelById(target, id, collect);
    }
    
    /**
     * 添加喜欢歌单
     *
     * @param userId          用户
     * @param id              音乐ID
     * @param isAddAndDelLike true添加 false删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectLike(Long userId, Long id, Boolean isAddAndDelLike) {
        LambdaQueryWrapper<TbCollectPojo> query = Wrappers.lambdaQuery();
        query.eq(TbCollectPojo::getUserId, userId);
        query.eq(TbCollectPojo::getType, PlayListTypeConfig.LIKE);
        TbCollectPojo collectServiceById = collectService.getOne(query);
        if (collectServiceById == null) {
            // 添加用户喜爱歌单
            TbCollectPojo entity = new TbCollectPojo();
            SysUserPojo userPojo = accountService.getById(userId);
            entity.setPlayListName(userPojo.getNickname() + " 喜欢的音乐");
            entity.setSort(collectService.count());
            entity.setUserId(userId);
            entity.setType(PlayListTypeConfig.LIKE);
            collectService.save(entity);
            this.saveOrUpdateCollectPic(entity.getId(), this.getUserAvatarPicUrl(userId));
            collectServiceById = entity;
        }
        TbMusicPojo byId = musicService.getById(id);
        if (byId == null) {
            log.debug("添加歌曲不存在: {}", id);
            throw new BaseException(ResultCode.SONG_NOT_EXIST);
        }
        
        // 效验歌单中是否有该歌曲
        LambdaQueryWrapper<TbCollectMusicPojo> wrapper = Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                                 .eq(TbCollectMusicPojo::getCollectId, collectServiceById.getId())
                                                                 .eq(TbCollectMusicPojo::getMusicId, id);
        long count = collectMusicService.count(wrapper);
        // 删除还是添加歌曲
        if (Boolean.TRUE.equals(isAddAndDelLike)) {
            // 歌曲已存在
            if (count >= 1) {
                throw new BaseException(ResultCode.SONG_EXIST);
            }
            TbCollectMusicPojo tbLikeMusicPojo = new TbCollectMusicPojo();
            tbLikeMusicPojo.setCollectId(collectServiceById.getId());
            tbLikeMusicPojo.setMusicId(id);
            
            Long sort = getCollectCount(collectServiceById.getId());
            tbLikeMusicPojo.setSort(sort);
            collectMusicService.save(tbLikeMusicPojo);
            log.debug("歌单ID: {} 歌曲ID: {}  歌曲保存", tbLikeMusicPojo.getCollectId(), tbLikeMusicPojo.getMusicId());
            
            TbCollectPojo entity = new TbCollectPojo();
            entity.setId(collectServiceById.getId());
            this.saveOrUpdateCollectPic(entity.getId(), this.getMusicPicUrl(byId.getId()));
            collectService.updateById(entity);
        } else {
            // 歌曲不存在
            if (count == 0) {
                throw new BaseException(ResultCode.SONG_NOT_EXIST);
            }
            collectMusicService.remove(wrapper);
            log.debug("歌单ID: {} 歌曲ID: {}  歌曲已删除", collectServiceById.getId(), id);
        }
        
    }
    
    private Long getCollectCount(Long id) {
        LambdaQueryWrapper<TbCollectMusicPojo> eq = Wrappers.<TbCollectMusicPojo>lambdaQuery().
                                                            eq(TbCollectMusicPojo::getCollectId, id)
                                                            .orderByDesc(TbCollectMusicPojo::getSort);
        Page<TbCollectMusicPojo> page = collectMusicService.page(new Page<>(0, 1), eq);
        Long sort = 0L;
        if (CollUtil.isNotEmpty(page.getRecords()) && page.getRecords().size() == 1) {
            sort = page.getRecords().get(0).getSort();
            sort++;
        }
        return sort;
    }
    
    /**
     * 删除音乐
     *
     * @param musicId 音乐ID
     * @param compel  是否强制删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMusic(List<Long> musicId, Boolean compel) {
        if (CollUtil.isEmpty(musicId)) {
            return;
        }
        List<TbMusicPojo> musicList = musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getId, musicId));
        if (CollUtil.isEmpty(musicList)) {
            throw new BaseException(ResultCode.SONG_NOT_EXIST);
        }
        // 删除歌单
        LambdaQueryWrapper<TbCollectMusicPojo> queryWrapper1 = Wrappers.lambdaQuery();
        queryWrapper1.in(TbCollectMusicPojo::getMusicId, musicId);
        List<TbCollectMusicPojo> list = collectMusicService.list(queryWrapper1);
        // 是否强制删除歌单中的音乐
        if (CollUtil.isEmpty(list) || Boolean.TRUE.equals(compel)) {
            collectMusicService.remove(queryWrapper1);
        } else {
            throw new BaseException(ResultCode.COLLECT_MUSIC_ERROR);
        }
        
        // 删除专辑
        // 删除歌曲
        musicService.removeBatchByIds(musicId);
        // 删除音源
        LambdaQueryWrapper<TbResourcePojo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(TbResourcePojo::getMusicId, musicId);
        musicUrlService.remove(queryWrapper);
        // 删除Tag中间表
        middleTagService.removeBatchByIds(musicId);
        // 删除封面
        removePicIds(musicId);
    }
    
    
    /**
     * 删除专辑
     * 强制删除会删除歌曲表
     *
     * @param id     专辑ID 列表
     * @param compel 是否强制删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(List<Long> id, Boolean compel) {
        if (CollUtil.isEmpty(id)) {
            return;
        }
        // 检测是否存在专辑
        long count = albumService.count(Wrappers.<TbAlbumPojo>lambdaQuery().in(TbAlbumPojo::getId, id));
        if (count == 0) {
            throw new BaseException(ResultCode.ALBUM_NO_EXIST_ERROR);
        }
        // 检测专辑是否包括音乐
        List<MusicConvert> musicListByAlbumId = getMusicListByAlbumId(id);
        if (CollUtil.isEmpty(musicListByAlbumId) || Boolean.TRUE.equals(compel)) {
            // 删除封面
            List<TbAlbumPojo> albumPojoList = albumService.listByIds(id);
            removePicIds(id);
            // 删除tag
            albumPojoList.stream().map(TbAlbumPojo::getId).filter(Objects::nonNull).forEach(this::removeLabelAll);
            albumService.removeByIds(id);
        } else {
            throw new BaseException(ResultCode.ALBUM_MUSIC_EXIST_ERROR);
        }
        // 强制删除音乐
        if (CollUtil.isNotEmpty(musicListByAlbumId) && Boolean.TRUE.equals(compel)) {
            List<Long> collect = musicListByAlbumId.parallelStream().map(TbMusicPojo::getId).toList();
            deleteMusic(collect, true);
        }
        
    }
    
    /**
     * 删除歌手
     *
     * @param id 歌手ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArtist(List<Long> id) {
        if (CollUtil.isEmpty(id)) {
            return;
        }
        Wrapper<TbArtistPojo> wrapper = Wrappers.<TbArtistPojo>lambdaQuery().in(TbArtistPojo::getId, id);
        long count = artistService.count(wrapper);
        if (count == 0) {
            throw new BaseException(ResultCode.ARTIST_NO_EXIST_ERROR);
        }
        LambdaQueryWrapper<TbAlbumArtistPojo> in = Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getArtistId, id);
        albumArtistService.remove(in);
        artistService.remove(wrapper);
        // 删除封面
        removePicIds(id);
    }
    
    /**
     * 保存或更新歌词
     *
     * @param musicId 音乐ID
     * @param type    歌词类型
     * @param lyric   歌词
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateLyric(Long musicId, String type, String lyric) {
        if (!(StringUtils.equals(type, LyricConfig.LYRIC) ||
                StringUtils.equals(type, LyricConfig.K_LYRIC) ||
                StringUtils.equals(type, LyricConfig.T_LYRIC))) {
            throw new BaseException(ResultCode.LYRIC_NO_EXIST_EXISTED);
        }
        TbLyricPojo one = lyricService.getOne(Wrappers.<TbLyricPojo>lambdaQuery()
                                                      .eq(TbLyricPojo::getMusicId, musicId)
                                                      .eq(TbLyricPojo::getType, LyricConfig.LYRIC));
        TbLyricPojo entity = Optional.ofNullable(one).orElse(new TbLyricPojo());
        entity.setMusicId(musicId);
        entity.setType(LyricConfig.LYRIC);
        entity.setLyric(lyric);
        lyricService.saveOrUpdate(entity);
    }
    
    public List<MusicConvert> getPicMusicList(Collection<TbMusicPojo> musicList) {
        Set<Long> collect = musicList.parallelStream().map(TbMusicPojo::getId).collect(Collectors.toSet());
        Map<Long, String> picUrl = getCollectPicUrl(collect);
        return musicList.parallelStream().map(tbMusicPojo -> {
            String url = picUrl.get(tbMusicPojo.getId());
            MusicConvert musicConvert = new MusicConvert();
            musicConvert.setPicUrl(url);
            BeanUtils.copyProperties(tbMusicPojo, musicConvert);
            return musicConvert;
        }).toList();
    }
    
    public List<AlbumConvert> getPicAlbumList(Collection<TbAlbumPojo> musicList) {
        Set<Long> collect = musicList.parallelStream().map(TbAlbumPojo::getId).collect(Collectors.toSet());
        Map<Long, String> picUrl = getAlbumPicUrl(collect);
        return musicList.parallelStream().map(tbMusicPojo -> {
            String url = picUrl.get(tbMusicPojo.getId());
            AlbumConvert musicConvert = new AlbumConvert();
            musicConvert.setPicUrl(url);
            BeanUtils.copyProperties(tbMusicPojo, musicConvert);
            return musicConvert;
        }).toList();
    }
    
    public List<ArtistConvert> getPicArtistList(Collection<TbArtistPojo> musicList) {
        Set<Long> collect = musicList.parallelStream().map(TbArtistPojo::getId).collect(Collectors.toSet());
        Map<Long, String> picUrl = getCollectPicUrl(collect);
        return musicList.parallelStream().map(tbMusicPojo -> {
            String url = picUrl.get(tbMusicPojo.getId());
            ArtistConvert musicConvert = new ArtistConvert();
            musicConvert.setPicUrl(url);
            BeanUtils.copyProperties(tbMusicPojo, musicConvert);
            return musicConvert;
        }).toList();
    }
    
    
    /**
     * 封面
     *
     * @param ids  封面ID
     * @param type 关联ID类型
     * @return 封面地址
     */
    @Override
    public Map<Long, String> getPicUrl(Collection<Long> ids, Byte type) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        // 通过关联ID获取封面ID
        Map<Long, Long> picMiddle = picMiddleCache.getAll(ids, aLong -> {
            List<TbMiddlePicPojo> list = middlePicService.list();
            return list.stream().collect(Collectors.toMap(TbMiddlePicPojo::getMiddleId, TbMiddlePicPojo::getPicId));
        });
        // 返回默认地址
        if (CollUtil.isEmpty(picMiddle)) {
            return ids.parallelStream().collect(Collectors.toMap(Long::longValue, aLong -> getDefaultPicUrl(type)));
        }
        // 获取缓存中地址
        List<Long> picIds = ids.parallelStream().map(picMiddle::get).filter(Objects::nonNull).toList();
        Map<Long, TbPicPojo> map = picCache.getAll(picIds, longs -> {
            List<TbMiddlePicPojo> list = middlePicService.list(Wrappers.<TbMiddlePicPojo>lambdaQuery()
                                                                       .in(TbMiddlePicPojo::getMiddleId, ids)
                                                                       .in(TbMiddlePicPojo::getPicId, longs)
                                                                       .eq(TbMiddlePicPojo::getType, type));
            List<TbPicPojo> tbPicPojoList = picService.listByIds(list.parallelStream().map(TbMiddlePicPojo::getPicId).collect(Collectors.toSet()));
            return tbPicPojoList.parallelStream().map(tbPicPojo -> {
                tbPicPojo = tbPicPojo == null ? new TbPicPojo() : tbPicPojo;
                if (StringUtils.isEmpty(tbPicPojo.getUrl())) {
                    tbPicPojo.setUrl(getDefaultPicUrl(type));
                }
                return tbPicPojo;
            }).collect(Collectors.toMap(TbPicPojo::getId, tbPicPojo -> tbPicPojo));
        });
        // 遍历ID，如果没有查找到，则返回默认数据
        return ids.parallelStream().collect(Collectors.toMap(o -> o, aLong -> {
            Long picId = picMiddle.get(aLong);
            return picId == null ? getDefaultPicUrl(type) : map.get(picId).getUrl();
        }));
    }
    
    private String getDefaultPicUrl(Byte type) {
        return switch (type) {
            case PicTypeConstant.MUSIC -> defaultInfo.getPic().getMusicPic();
            case PicTypeConstant.PLAYLIST -> defaultInfo.getPic().getPlayListPic();
            case PicTypeConstant.ALBUM -> defaultInfo.getPic().getAlbumPic();
            case PicTypeConstant.ARTIST -> defaultInfo.getPic().getArtistPic();
            case PicTypeConstant.USER_AVATAR -> defaultInfo.getPic().getUserAvatarPic();
            case PicTypeConstant.USER_BACKGROUND -> defaultInfo.getPic().getUserBackgroundPic();
            default -> defaultInfo.getPic().getDefaultPic();
        };
    }
    
    /**
     * 保存封面
     *
     * @param id   添加封面关联ID,
     * @param type 添加ID类型 歌曲，专辑，歌单，歌手
     * @param pojo 封面数据
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePic(Long id, Byte type, TbPicPojo pojo) {
        // 查询封面是否存在, 不存在则创建。存在则创建中间表关联
        Wrapper<TbPicPojo> eq = Wrappers.<TbPicPojo>lambdaQuery().eq(TbPicPojo::getMd5, pojo.getMd5());
        TbPicPojo one = picService.getOne(eq);
        TbMiddlePicPojo entity;
        if (one == null) {
            picService.save(pojo);
            entity = new TbMiddlePicPojo();
            entity.setMiddleId(id);
            entity.setType(type);
            entity.setPicId(pojo.getId());
        } else {
            Wrapper<TbMiddlePicPojo> lambdaQuery = Wrappers.<TbMiddlePicPojo>lambdaQuery()
                                                           .eq(TbMiddlePicPojo::getPicId, one.getId())
                                                           .eq(TbMiddlePicPojo::getMiddleId, id)
                                                           .eq(TbMiddlePicPojo::getType, type);
            TbMiddlePicPojo one1 = middlePicService.getOne(lambdaQuery);
            entity = one1 == null ? new TbMiddlePicPojo() : one1;
            entity.setMiddleId(id);
            entity.setType(type);
            entity.setPicId(one.getId());
        }
        middlePicService.saveOrUpdate(entity);
    }
    
    /**
     * 批量删除封面文件
     *
     * @param ids      封面
     * @param consumer 删除文件
     */
    protected void removePicFile(Collection<Long> ids, Consumer<List<String>> consumer) {
        middlePicService.remove(Wrappers.<TbMiddlePicPojo>lambdaQuery().in(TbMiddlePicPojo::getMiddleId, ids));
        // 删除所有没有引用封面
        List<TbMiddlePicPojo> middlePicList = middlePicService.list();
        List<TbPicPojo> picList = picService.list();
        List<Long> picIds = picList.parallelStream().map(TbPicPojo::getId).toList();
        List<Long> middlePicIds = middlePicList.parallelStream().map(TbMiddlePicPojo::getPicId).toList();
        Collection<Long> subtract = CollUtil.subtract(picIds, middlePicIds);
        picService.removeByIds(subtract);
        List<String> collect = picList.parallelStream()
                                      .filter(pojo -> subtract.contains(pojo.getId()))
                                      .map(TbPicPojo::getUrl)
                                      .toList();
        Set<Long> removeIds = picList.parallelStream().map(TbPicPojo::getId).filter(subtract::contains).collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(collect)) {
            picService.removeByIds(removeIds);
            consumer.accept(collect);
        }
    }
    
    /**
     * 删除封面数据
     *
     * @param id 封面
     */
    public void removePic(Long id) {
        this.removePicFile(Collections.singletonList(id), null);
    }
    
    /**
     * @param picIds 封面数据
     */
    @Override
    public void removePicIds(List<Long> picIds) {
        removePicFile(picIds, null);
    }
    
}
