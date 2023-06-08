package org.web.controller.admin.v1;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.api.admin.config.AdminConfig;
import org.api.admin.model.req.MusicInfoReq;
import org.api.admin.model.req.UploadMusicReq;
import org.api.admin.model.req.upload.AudioInfoReq;
import org.api.admin.service.MusicFlowApi;
import org.core.common.result.R;
import org.core.pojo.MusicDetails;
import org.core.pojo.TbMusicUrlPojo;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController(AdminConfig.ADMIN + "MusicController")
@RequestMapping("/admin/music")
@Slf4j
@CrossOrigin
public class MusicController {
    private final MusicFlowApi uploadMusic;
    
    public MusicController(MusicFlowApi uploadMusic) {
        this.uploadMusic = uploadMusic;
    }
    
    /**
     * 上传临时文件
     *
     * @param uploadFile 临时文件
     * @return 返回音乐数据
     */
    @PostMapping("/upload/file")
    public R uploadMusicFile(@RequestParam(value = "file", required = false) MultipartFile uploadFile, @RequestParam(value = "url", required = false) String url) throws CannotReadException, TagException, ReadOnlyFileException, IOException {
        return R.success(uploadMusic.uploadMusicFile(uploadFile, url));
    }
    
    /**
     * 获取临时上传音乐字节数据
     *
     * @param musicTempFile 临时文件
     * @return 字节数据
     */
    @GetMapping("/get/temp/{music}")
    public ResponseEntity<FileSystemResource> getMusicTempFile(@PathVariable("music") String musicTempFile) {
        return uploadMusic.getMusicTempFile(musicTempFile);
    }
    
    /**
     * 上传音乐信息，包括临时文件名
     *
     * @param dto 音乐信息
     * @return 返回成功信息
     */
    @PostMapping("/upload/info")
    public R uploadMusicInfo(@Validated @RequestBody AudioInfoReq dto) {
        MusicDetails musicDetails = uploadMusic.saveMusicInfo(dto);
        return R.success(musicDetails);
    }
    
    /**
     * 获取音乐URL
     *
     * @param musicId 音乐id
     * @param refresh 是否刷新
     * @return url
     */
    @GetMapping("/url/{musicId}")
    public R getMusicUrl(@PathVariable("musicId") Set<String> musicId, @RequestParam(value = "refresh", required = false, defaultValue = "false") Boolean refresh) {
        return R.success(uploadMusic.getMusicUrl(musicId, refresh));
    }
    
    /**
     * 获取歌词
     *
     * @param musicId 音乐ID
     * @return 歌词列表
     */
    @GetMapping("/lyric/{musicId}")
    public R getMusicLyric(@PathVariable("musicId") Long musicId) {
        return R.success(uploadMusic.getMusicLyric(musicId));
    }
    
    
    /**
     * 删除音乐
     *
     * @param musicId 音乐ID
     * @param compel  是否强制删除
     * @return 成功信息
     */
    @DeleteMapping("/{id}")
    public R deleteMusic(@PathVariable("id") List<Long> musicId, @RequestParam(value = "compel", required = false, defaultValue = "false") Boolean compel) {
        uploadMusic.deleteMusic(musicId, compel);
        return R.success();
    }
    
    /**
     * 更新或保存歌词
     *
     * @param musicId 音乐ID
     * @param type    歌词类型
     * @param lyric   歌词
     * @return 成功信息
     */
    @PostMapping("/lyric/{musicId}")
    public R saveOrUpdateLyric(@PathVariable("musicId") Long musicId, @RequestParam("type") String type, @RequestBody Map<String, String> lyric) {
        uploadMusic.saveOrUpdateLyric(musicId, type, MapUtil.get(lyric, "lyric", String.class));
        return R.success();
    }
    
    /**
     * 获取歌词信息
     *
     * @param id 歌曲ID
     * @return 歌曲信息
     */
    @GetMapping("/musicInfo/{id}")
    public R getMusicInfo(@PathVariable("id") Long id) {
        return R.success(uploadMusic.getMusicInfo(id));
    }
    
    /**
     * 更新音乐信息
     *
     * @param req 音乐信息
     * @return 成功信息
     */
    @PostMapping
    public R updateMusic(@RequestBody MusicInfoReq req) {
        uploadMusic.saveOrUpdateMusic(req);
        return R.success();
    }
    
    /**
     * 自动上传音乐文件
     *
     * @param userId     用户ID
     * @param uploadFile 上传文件
     * @param musicId    音乐ID
     * @return 成功信息
     */
    @PostMapping("/auto/upload")
    public R uploadAutoMusic(@RequestParam("userId") Long userId, @RequestParam(value = "file", required = false) MultipartFile uploadFile, @RequestParam("id") Long musicId) {
        uploadMusic.uploadAutoMusic(userId, uploadFile, musicId);
        return R.success();
    }
    
    /**
     * 手动上传音乐文件
     *
     * @param musicSource 音乐信息
     * @return 成功信息
     */
    @PostMapping("/manual/upload")
    public R uploadManualMusic(@RequestBody UploadMusicReq musicSource) {
        uploadMusic.uploadManualMusic(musicSource);
        return R.success();
    }
    
    /**
     * 更新音乐 音源
     *
     * @param source 音源信息
     * @return 成功信息
     */
    @PostMapping("/update/source")
    public R updateSource(@RequestBody TbMusicUrlPojo source) {
        uploadMusic.updateSource(source);
        return R.success();
    }
    
    /**
     * 删除音源
     *
     * @param id 音源ID
     * @return 成功信息
     */
    @DeleteMapping("/delete/source/{id}")
    public R deleteSource(@PathVariable("id") Long id) {
        uploadMusic.deleteSource(id);
        return R.success();
    }
}
