package org.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.config.TargetTagConfig;
import org.core.iservice.*;
import org.core.pojo.*;
import org.core.service.QukuService;
import org.core.utils.CollectSortUtil;
import org.core.utils.ExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("MusicService")
@Slf4j
public class QukuServiceImpl implements QukuService {
    
    @Autowired
    private TbMusicService musicService;
    
    @Autowired
    private TbAlbumService albumService;
    
    @Autowired
    private TbArtistService artistService;
    
    /**
     * 音乐地址服务
     */
    @Autowired
    private TbMusicUrlService musicUrlService;
    
    @Autowired
    private TbUserAlbumService userAlbumService;
    
    @Autowired
    private TbAlbumArtistService albumArtistService;
    
    @Autowired
    private TbUserArtistService userSingerService;
    
    @Autowired
    private TbCollectMusicService collectMusicService;
    
    @Autowired
    private TbCollectService collectService;
    
    @Autowired
    private TbCollectMusicTagService collectMusicTagService;
    
    @Autowired
    private TbLyricService lyricService;
    
    @Autowired
    private TbTagService tagService;
    
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
    public List<TbAlbumPojo> getAlbumListByAlbumId(Collection<Long> albumIds) {
        return albumService.listByIds(albumIds);
    }
    
    /**
     * 批量获取歌手信息
     * Long -> music ID
     */
    @Override
    public List<TbArtistPojo> getArtistListByMusicId(List<Long> musicIds) {
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
    public Map<Long, List<TbArtistPojo>> getArtistListByMusicIdToMap(Map<Long, TbAlbumPojo> albumPojoMap) {
        Set<Long> collect = albumPojoMap.values().parallelStream().map(TbAlbumPojo::getId).collect(Collectors.toSet());
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
        Map<Long, TbArtistPojo> artistMap = artistPojoList.parallelStream().collect(Collectors.toMap(TbArtistPojo::getId, tbArtistPojo -> tbArtistPojo));
        
        Map<Long, List<TbArtistPojo>> resMap = new HashMap<>();
        for (Map.Entry<Long, TbAlbumPojo> longTbAlbumPojoEntry : albumPojoMap.entrySet()) {
            TbAlbumPojo tbAlbumPojo = longTbAlbumPojoEntry.getValue();
            List<TbAlbumArtistPojo> tbAlbumArtistPojos = albumArtistMap.get(tbAlbumPojo.getId()) == null
                    ? new ArrayList<>()
                    : albumArtistMap.get(tbAlbumPojo.getId());
            List<TbArtistPojo> tbArtistPojos = tbAlbumArtistPojos.parallelStream()
                                                                 .map(tbAlbumArtistPojo -> artistMap.get(tbAlbumArtistPojo.getArtistId()))
                                                                 .collect(Collectors.toList());
            resMap.put(longTbAlbumPojoEntry.getKey(), tbArtistPojos);
        }
        return resMap;
    }
    
    /**
     * 获取歌手信息
     */
    @Override
    public List<TbArtistPojo> getArtistByMusicId(Long musicId) {
        return getArtistListByMusicId(Collections.singletonList(musicId));
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
     * 获取专辑歌手列表
     */
    @Override
    public List<TbArtistPojo> getArtistListByAlbumIds(Long albumIds) {
        return getArtistListByAlbumIds(Collections.singletonList(albumIds));
    }
    
    @Override
    public List<TbAlbumPojo> getAlbumListByArtistIds(List<Long> ids) {
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
    public List<TbArtistPojo> getArtistListByAlbumIds(List<Long> albumIds) {
        List<TbAlbumArtistPojo> list = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery().in(TbAlbumArtistPojo::getAlbumId, albumIds));
        return getTbSingerPojoList(CollUtil.isEmpty(list), list.stream().map(TbAlbumArtistPojo::getArtistId));
    }
    
    /**
     * 通过专辑ID获取歌手
     *
     * @param empty      是否执行
     * @param longStream 专辑ID流
     */
    private List<TbArtistPojo> getTbSingerPojoList(boolean empty, Stream<Long> longStream) {
        if (empty) {
            return Collections.emptyList();
        }
        List<Long> singerIds = longStream.collect(Collectors.toList());
        return artistService.list(Wrappers.<TbArtistPojo>lambdaQuery().in(TbArtistPojo::getId, singerIds));
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
    public List<TbArtistPojo> getUserLikeSingerList(SysUserPojo user) {
        List<TbUserArtistPojo> userLikeSinger = userSingerService.list(Wrappers.<TbUserArtistPojo>lambdaQuery()
                                                                               .eq(TbUserArtistPojo::getUserId, user.getId()));
        if (CollUtil.isEmpty(userLikeSinger)) {
            return Collections.emptyList();
        }
        List<Long> singerIds = userLikeSinger.stream().map(TbUserArtistPojo::getArtistId).collect(Collectors.toList());
        return artistService.listByIds(singerIds);
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
     * 根据专辑ID查找音乐
     *
     * @param id 专辑ID
     */
    @Override
    public List<TbMusicPojo> getMusicListByAlbumId(Long id) {
        return musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().eq(TbMusicPojo::getAlbumId, id));
    }
    
    @Override
    public List<TbMusicPojo> getMusicListByAlbumId(Collection<Long> ids) {
        return musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getAlbumId, ids));
    }
    
    /**
     * 根据歌手名查找音乐
     *
     * @param name 歌手
     */
    @Override
    public List<TbMusicPojo> getMusicListByArtistName(String name) {
        if (StringUtils.isNotBlank(name)) {
            List<TbArtistPojo> singerList = artistService.list(Wrappers.<TbArtistPojo>lambdaQuery().like(TbArtistPojo::getArtistName, name));
            List<Long> singerIdsList = singerList.stream().map(TbArtistPojo::getId).collect(Collectors.toList());
            List<TbAlbumArtistPojo> albumIds = albumArtistService.list(Wrappers.<TbAlbumArtistPojo>lambdaQuery()
                                                                               .eq(TbAlbumArtistPojo::getArtistId, singerIdsList));
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
    public List<TbMusicPojo> getMusicListByArtistId(Long id) {
        List<TbAlbumPojo> albumListBySingerIds = getAlbumListByArtistIds(Collections.singletonList(id));
        if (CollUtil.isEmpty(albumListBySingerIds)) {
            return Collections.emptyList();
        }
        Set<Long> albumIds = albumListBySingerIds.stream().map(TbAlbumPojo::getId).collect(Collectors.toSet());
        return musicService.list(Wrappers.<TbMusicPojo>lambdaQuery().in(TbMusicPojo::getAlbumId, albumIds));
    }
    
    /**
     * 随机获取歌手
     *
     * @param count 获取数量
     */
    @Override
    public List<TbArtistPojo> randomSinger(int count) {
        long sum = artistService.count();
        ArrayList<TbArtistPojo> res = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long randomNum = RandomUtil.randomLong(sum);
            Page<TbArtistPojo> page = artistService.page(new Page<>(randomNum, 1));
            res.addAll(page.getRecords());
        }
        return res;
    }
    
    /**
     * 添加音乐到歌单
     *
     * @param userID        用户ID
     * @param tbCollectPojo 歌单数据
     * @param songIds       歌曲列表
     * @param flag          删除还是添加
     */
    @Override
    public void addMusicToCollect(Long userID, TbCollectPojo tbCollectPojo, List<Long> songIds, boolean flag) {
        if (flag) {
            // 查询歌单内歌曲是否存在
            long count = collectMusicService.count(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                           .eq(TbCollectMusicPojo::getCollectId, tbCollectPojo.getId())
                                                           .in(TbCollectMusicPojo::getMusicId, songIds));
            ExceptionUtil.isNull(count > 0, ResultCode.PLAT_LIST_MUSIC_EXIST);
            
            // 添加
            List<TbMusicPojo> tbMusicPojo = musicService.listByIds(songIds);
            
            long allCount = collectMusicService.count(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                                              .eq(TbCollectMusicPojo::getCollectId, tbCollectPojo.getId()));
            List<TbCollectMusicPojo> collect = new ArrayList<>(tbMusicPojo.size());
            for (TbMusicPojo musicPojo : tbMusicPojo) {
                TbCollectMusicPojo tbCollectMusicPojo = new TbCollectMusicPojo();
                tbCollectMusicPojo.setCollectId(tbCollectPojo.getId());
                tbCollectMusicPojo.setMusicId(musicPojo.getId());
                allCount++;
                tbCollectMusicPojo.setSort(allCount);
                collect.add(tbCollectMusicPojo);
            }
            collectMusicService.saveBatch(collect);
            Long songId = songIds.get(songIds.size() - 1);
            // 更新封面
            TbMusicPojo musicPojo = musicService.getById(songId);
            if (musicPojo != null) {
                tbCollectPojo.setPic(musicPojo.getPic());
            }
            collectService.updateById(tbCollectPojo);
        } else {
            // 删除歌曲
            collectMusicService.remove(Wrappers.<TbCollectMusicPojo>lambdaQuery()
                                               .eq(TbCollectMusicPojo::getCollectId, tbCollectPojo.getId())
                                               .in(TbCollectMusicPojo::getMusicId, songIds));
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
    public TbCollectPojo createPlayList(Long userId, String name, short type) {
        TbCollectPojo collectPojo = new TbCollectPojo();
        collectPojo.setUserId(userId);
        collectPojo.setPlayListName(name);
        collectPojo.setSort(collectService.count() + 1);
        collectPojo.setPic("https://p1.music.126.net/jWE3OEZUlwdz0ARvyQ9wWw==/109951165474121408.jpg");
        collectPojo.setSubscribed(false);
        collectPojo.setType(type);
        collectService.save(collectPojo);
        return collectPojo;
    }
    
    /**
     * 检查用户是否有权限操作歌单
     *
     * @param userId        用户ID
     * @param tbCollectPojo 歌单信息
     */
    private static void checkUserAuth(Long userId, TbCollectPojo tbCollectPojo) {
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
     * 删除歌单
     *
     * @param userId      用户ID
     * @param collectList 删除歌单ID
     */
    @Override
    public void removePlayList(Long userId, Collection<Long> collectList) {
        List<TbCollectPojo> tbCollectPojos = collectService.listByIds(collectList);
        for (TbCollectPojo tbCollectPojo : tbCollectPojos) {
            checkUserAuth(userId, tbCollectPojo);
        }
        // 删除歌单关联ID
        collectMusicService.remove(Wrappers.<TbCollectMusicPojo>lambdaQuery().in(TbCollectMusicPojo::getCollectId, collectList));
        // 删除歌单ID
        collectService.removeByIds(collectList);
        // 删除歌单关联tag
        collectMusicTagService.remove(Wrappers.<TbCollectMusicTagPojo>lambdaQuery()
                                              .eq(TbCollectMusicTagPojo::getType, TargetTagConfig.TARGET_COLLECT_TAG)
                                              .in(TbCollectMusicTagPojo::getId, collectList));
    }
    
    /**
     * 获取用户所有音乐，包括喜爱歌单
     *
     * @param uid  用户ID
     * @param type 歌单类型
     * @return 返回用户创建歌单
     */
    @Override
    public List<TbCollectPojo> getUserPlayList(Long uid, Collection<Short> type) {
        LambdaQueryWrapper<TbCollectPojo> queryWrapper = Wrappers.<TbCollectPojo>lambdaQuery()
                                                                 .eq(TbCollectPojo::getUserId, uid);
        List<TbCollectPojo> list = collectService.list(queryWrapper);
        return CollectSortUtil.userLikeUserSort(uid, list);
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
    public void addLabel(Short target, Long id, String label) {
        TbTagPojo tagPojo = tagService.getOne(Wrappers.<TbTagPojo>lambdaQuery().eq(TbTagPojo::getTagName, label));
        Optional<TbTagPojo> pojo = Optional.ofNullable(tagPojo);
        tagPojo = pojo.orElse(new TbTagPojo());
        if (pojo.isPresent()) {
            tagPojo.setTagName(label);
            tagService.saveOrUpdate(tagPojo);
        }
        addLabel(target, id, tagPojo.getId());
    }
    
    /**
     * 对歌单tag，音乐添加tag， 或者指定音乐流派
     *
     * @param target  指定歌单tag，或者音乐tag，音乐流派 0流派 1歌曲 2歌单
     * @param id      歌单或歌曲前ID
     * @param labelId 标签ID
     */
    @Override
    public void addLabel(Short target, Long id, Long labelId) {
        LambdaQueryWrapper<TbCollectMusicTagPojo> eq = Wrappers.<TbCollectMusicTagPojo>lambdaQuery()
                                                               .eq(TbCollectMusicTagPojo::getType, target)
                                                               .eq(TbCollectMusicTagPojo::getId, id);
        TbCollectMusicTagPojo one = collectMusicTagService.getOne(eq);
        one = Optional.ofNullable(one).orElse(new TbCollectMusicTagPojo());
        one.setId(id);
        one.setTagId(labelId);
        one.setType(target);
        collectMusicTagService.save(one);
    }
}
