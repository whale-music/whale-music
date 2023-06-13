package org.api.admin.task.timer;


import cn.hutool.core.collection.CollUtil;
import org.api.admin.common.constant.TimeDeleteTask;
import org.api.common.service.QukuAPI;
import org.core.iservice.TbMiddleTagService;
import org.core.iservice.TbTagService;
import org.core.pojo.TbMiddleTagPojo;
import org.core.pojo.TbTagPojo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PicTimeDeleteTask {
    private final QukuAPI qukuAPI;
    
    private final TbMiddleTagService middleTagService;
    
    private final TbTagService tagService;
    
    public PicTimeDeleteTask(QukuAPI qukuAPI, TbMiddleTagService middleTagService, TbTagService tagService) {
        this.qukuAPI = qukuAPI;
        this.middleTagService = middleTagService;
        this.tagService = tagService;
    }
    
    @Scheduled(cron = TimeDeleteTask.CRON)   // 每天00:00执行一次
    public void autoDeletePic() {
        // List<TbMusicPojo> musicList = musicService.list();
        // List<TbAlbumPojo> albumList = albumService.list();
        // List<TbArtistPojo> artistList = artistService.list();
        // List<TbCollectPojo> collectList = collectService.list();
        //
        // List<TbPicPojo> list = picService.list();
        // List<Long> picIds = list.parallelStream().map(TbPicPojo::getId).collect(Collectors.toList());
        // Set<Long> musicPicIds = musicList.parallelStream().map(TbMusicPojo::getId).collect(Collectors.toSet());
        // Set<Long> albumPicIds = albumList.parallelStream().map(TbAlbumPojo::getId).collect(Collectors.toSet());
        // Set<Long> artistPicIds = artistList.parallelStream().map(TbArtistPojo::getId).collect(Collectors.toSet());
        // Set<Long> collectPicIds = collectList.parallelStream().map(TbCollectPojo::getId).collect(Collectors.toSet());
        //
        // HashSet<Long> tempPicAll = new HashSet<>();
        // tempPicAll.addAll(musicPicIds);
        // tempPicAll.addAll(albumPicIds);
        // tempPicAll.addAll(artistPicIds);
        // tempPicAll.addAll(collectPicIds);
        // // 获取没有关联的封面，然后删除
        // List<Long> subtract = CollUtil.subtractToList(picIds, tempPicAll);
        // if (CollUtil.isNotEmpty(subtract)) {
        //     qukuAPI.removePicIds(subtract);
        // }
    }
    
    @Scheduled(cron = TimeDeleteTask.CRON)   // 每天00:00执行一次
    public void autoDeleteTag() {
        List<TbTagPojo> tagList = tagService.list();
        List<TbMiddleTagPojo> middleTagList = middleTagService.list();
        List<Long> tagIds = tagList.parallelStream().map(TbTagPojo::getId).collect(Collectors.toList());
        List<Long> middleTagIds = middleTagList.parallelStream().map(TbMiddleTagPojo::getMiddleId).collect(Collectors.toList());
        List<Long> subtract = CollUtil.subtractToList(tagIds, middleTagIds);
        if (CollUtil.isNotEmpty(subtract)) {
            subtract.parallelStream().forEach(qukuAPI::removeLabelAll);
        }
    }
}
