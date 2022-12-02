package org.api.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.api.admin.dto.AudioInfoDto;
import org.api.admin.vo.AudioInfoVo;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.config.MusicConfig;
import org.core.pojo.*;
import org.core.service.*;
import org.core.utils.LocalFileUtil;
import org.core.utils.UserUtil;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.oss.service.impl.LocalOSSServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UploadMusicApi {
    
    String[] fileType = {"mp3", "ogg", "flac"};
    /**
     * 音乐信息服务
     */
    @Autowired
    private TbMusicService musicService;
    /**
     * 音乐保存数据链接表
     */
    @Autowired
    private TbMusicUrlService musicUrlService;
    /**
     * 歌手服务
     */
    @Autowired
    private TbSingerService singerService;
    /**
     * 音乐和歌曲家中间表
     */
    @Autowired
    private TbMusicSingerService musicSingerService;
    /**
     * 专辑表
     */
    @Autowired
    private TbAlbumService albumService;
    /**
     * 上传配置
     */
    @Autowired
    private MusicConfig config;
    /**
     * 文件上传服务
     */
    @Autowired
    private LocalOSSServiceImpl localOSSService;
    
    String pathTemp = FileUtil.getTmpDirPath() + "\\musicTemp\\";
    
    /**
     * 上传文件到临时目录
     *
     * @param uploadFile 临时文件
     * @return 音乐信息
     */
    @Transactional(rollbackFor = Exception.class)
    public AudioInfoVo uploadMusicFile(MultipartFile uploadFile) throws IOException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException {
        String filename = uploadFile.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            throw new BaseException(ResultCode.FILENAME_INVALID);
        }
        int indexOf = filename.lastIndexOf('.');
        String[] split = filename.split(String.valueOf(new char[]{'\\', filename.charAt(indexOf)}));
        if (split.length < 1) {
            throw new BaseException(ResultCode.FILENAME_INVALID);
        }
        // 检测文件类型是否有效
        if (!StringUtils.containsAny(split[1], fileType)) {
            throw new BaseException(ResultCode.FILENAME_INVALID);
        }
        String musicFileName = UUID.fastUUID() + "." + split[1];
        String path = pathTemp + musicFileName;
        BufferedOutputStream outputStream = FileUtil.getOutputStream(path);
        outputStream.write(uploadFile.getBytes());
        outputStream.close();
        AudioFile read = AudioFileIO.read(new File(path));
        log.info(" ----- ----- ");
        log.info("标题:" + read.getTag().getFirst(FieldKey.TITLE));
        log.info("作者:" + read.getTag().getFirst(FieldKey.ARTIST));
        log.info("专辑:" + read.getTag().getFirst(FieldKey.ALBUM));
        log.info("比特率:" + read.getAudioHeader().getBitRate());
        log.info("时长:" + read.getAudioHeader().getTrackLength() + "s");
        log.info("大小:" + (read.getFile().length() / 1024F / 1024F) + "MB");
        log.info(" ----- ----- ");
        AudioInfoVo audioInfoVo = new AudioInfoVo();
        audioInfoVo.setMusicName(read.getTag().getFirst(FieldKey.TITLE));
        audioInfoVo.setOriginFileName(split[0]);
        audioInfoVo.setSinger(Collections.singletonList(read.getTag().getFirst(FieldKey.ARTIST)));
        audioInfoVo.setAlbum(read.getTag().getFirst(FieldKey.ALBUM));
        audioInfoVo.setTimeLength(read.getAudioHeader().getTrackLength());
        audioInfoVo.setSize(read.getFile().length());
        audioInfoVo.setMusicFileTemp(musicFileName);
        return audioInfoVo;
    }
    
    /**
     * 获取临时文件字节
     *
     * @param musicTempFile 临时文件名
     * @return 字节数据
     */
    public ResponseEntity<FileSystemResource> getMusicTempFile(String musicTempFile) {
        LocalFileUtil.checkFileNameLegal(musicTempFile);
        File file = LocalFileUtil.checkFilePath(pathTemp, musicTempFile);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        return ResponseEntity.ok()
                             .headers(headers)
                             .contentLength(file.length())
                             .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                             .body(new FileSystemResource(file));
    }
    
    /**
     * 保存音乐
     * 更新表: 音乐信息表 音乐地址表 歌手表 专辑表
     *
     * @param dto 音乐信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMusicInfo(AudioInfoDto dto) throws IOException {
        // 检查文件目录是否合法
        File file = LocalFileUtil.checkFilePath(pathTemp, dto.getMusicFileTemp());
        // 检测文件md5值是否一样，一样则不上传
        String md5 = DigestUtils.md5DigestAsHex(FileUtil.getInputStream(file));
        TbMusicUrlPojo one = musicUrlService.getOne(Wrappers.<TbMusicUrlPojo>lambdaQuery()
                                                            .eq(TbMusicUrlPojo::getMd5, md5));
        // 如果有该数据则表示数据库中已经有该数据了
        if (one != null) {
            throw new BaseException(ResultCode.SONG_EXIST);
        }
    
        /* 歌手表 */
        // 查询该音乐在歌手表中是否有数据, 没有数据则新增歌手
        LambdaQueryWrapper<TbSingerPojo> singerWrapper = Wrappers.<TbSingerPojo>lambdaQuery()
                                                                 .in(TbSingerPojo::getSingerName, dto.getSinger());
        List<TbSingerPojo> singList = singerService.list(singerWrapper);
        // 获取数据库中没有该歌手数据
        List<String> singNameList = singList.stream().map(TbSingerPojo::getSingerName).collect(Collectors.toList());
        List<String> singNameListDto = dto.getSinger()
                                          .stream()
                                          .map(TbSingerPojo::getSingerName)
                                          .filter(StringUtils::isNotBlank)
                                          .collect(Collectors.toList());
        Collection<String> intersection = CollUtil.disjunction(singNameListDto, singNameList);
        // 数据库中没有该歌手，更新歌手表
        if (intersection != null && !intersection.isEmpty()) {
            singList = dto.getSinger()
                          .stream()
                          .filter(singerDto -> intersection.contains(singerDto.getSingerName()))
                          .map(singerDto -> {
                              TbSingerPojo tbSingerPojo = new TbSingerPojo();
                              BeanUtils.copyProperties(singerDto, tbSingerPojo);
                              return tbSingerPojo;
                          })
                          .collect(Collectors.toList());
            singerService.saveBatch(singList);
        }
    
        /* 专辑表 */
        TbAlbumPojo albumPojo = albumService.getOne(Wrappers.<TbAlbumPojo>lambdaQuery()
                                                            .eq(TbAlbumPojo::getAlbumName, dto.getAlbum()
                                                                                              .getAlbumName()));
        // 如果没有数据则新增专辑表
        if (albumPojo == null) {
            albumPojo = new TbAlbumPojo();
            albumPojo.setId(IdWorker.getId());
            BeanUtils.copyProperties(dto.getAlbum(), albumPojo);
        }
        albumService.save(albumPojo);
    
        // music 和 歌手中间表
        long musicId = IdWorker.getId();
        // 在有新歌手没有录入数据库中的情况下，新增中间表
        if (intersection != null && !intersection.isEmpty()) {
            List<TbMusicSingerPojo> musicSingerList = singList.stream()
                                                              .map(tbSingerPojo -> {
                                                                  TbMusicSingerPojo tbMusicSingerPojo = new TbMusicSingerPojo();
                                                                  tbMusicSingerPojo.setMusicId(musicId);
                                                                  tbMusicSingerPojo.setSingerId(tbSingerPojo.getId());
                                                                  return tbMusicSingerPojo;
                                                              })
                                                              .collect(Collectors.toList());
            musicSingerService.saveBatch(musicSingerList);
        }
        // music 信息表
        TbMusicPojo musicPojo = new TbMusicPojo();
        musicPojo.setId(musicId);
        musicPojo.setMusicName(dto.getMusicName());
        musicPojo.setAliaName(dto.getAliaName());
        musicPojo.setPic(dto.getPic());
        musicPojo.setLyric(dto.getLyric());
        musicPojo.setAlbumId(albumPojo.getId());
        musicPojo.setSort(musicService.count());
        musicPojo.setTimeLength(dto.getTimeLength());
        // 保存音乐表
        boolean save = musicService.save(musicPojo);
        if (!save) {
            throw new BaseException(ResultCode.SAVE_FAIL);
        }
    
        // 上传文件
        String uploadPath = localOSSService.upload(file.getPath());
        Files.delete(file.toPath());
        // music URL 地址表
        TbMusicUrlPojo urlPojo = new TbMusicUrlPojo();
        urlPojo.setId(IdWorker.getId());
        urlPojo.setSize(FileUtil.size(file));
        urlPojo.setRate(dto.getRate());
        urlPojo.setQuality(dto.getQuality());
        urlPojo.setMd5(md5);
        urlPojo.setEncodeType(FileUtil.extName(file));
        urlPojo.setMusicId(musicPojo.getId());
        urlPojo.setUrl(uploadPath);
        urlPojo.setUserId(UserUtil.getUser().getId());
        musicUrlService.save(urlPojo);
    
    }
    
    /**
     * 查询音乐URL表
     *
     * @param musicId 音乐id
     * @return 音乐URL列表
     */
    public List<TbMusicUrlPojo> getMusicUrl(String musicId) {
        List<TbMusicUrlPojo> list = musicUrlService.list(Wrappers.<TbMusicUrlPojo>lambdaQuery()
                                                                 .eq(TbMusicUrlPojo::getId, musicId));
        if (list == null || list.isEmpty()) {
            throw new BaseException(ResultCode.SONG_NOT_EXIST);
        }
        for (TbMusicUrlPojo tbMusicUrlPojo : list) {
            tbMusicUrlPojo.setUrl(config.getHost() + tbMusicUrlPojo.getUrl());
        }
        return list;
    }
    
    /**
     * 下载音乐接口
     *
     * @param musicFilePath 音乐地址
     * @return 音乐数据
     */
    public ResponseEntity<FileSystemResource> downloadMusicFile(String musicFilePath) {
        LocalFileUtil.checkFileNameLegal(musicFilePath);
        File file = LocalFileUtil.checkFilePath(config.getObjectSave(), musicFilePath);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + musicFilePath);
        return ResponseEntity.ok()
                             .headers(headers)
                             .contentLength(file.length())
                             .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                             .body(new FileSystemResource(file));
    }
}
