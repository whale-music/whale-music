package org.web.subsonic.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.api.subsonic.ManualSerialize;
import org.api.subsonic.common.SubsonicCommonReq;
import org.api.subsonic.config.SubsonicConfig;
import org.api.subsonic.model.res.search.SearchRes;
import org.api.subsonic.model.res.search2.Search2Res;
import org.api.subsonic.model.res.search3.Search3Res;
import org.api.subsonic.service.SearchingApi;
import org.core.model.HttpStatusStr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "搜索")
@RestController(SubsonicConfig.SUBSONIC + "SearchingController")
@RequestMapping("/rest")
@Slf4j
@CrossOrigin(origins = "*")
public class SearchingController {
    
    @Autowired
    private SearchingApi searchingApi;
    
    @Operation(summary = "返回明星歌曲，专辑和艺术家")
    @ApiResponse(responseCode = HttpStatusStr.OK,
                 content = {
                         @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SearchRes.class)),
                         @Content(mediaType = MediaType.APPLICATION_XML_VALUE, schema = @Schema(implementation = SearchRes.class))
                 }
    )
    @GetMapping({"/search.view", "/search"})
    @ManualSerialize
    public ResponseEntity<String> search(SubsonicCommonReq req,
                                         @Parameter(description = "艺术家寻找")
                                         @RequestParam(value = "artist", required = false) String artist,
                                         
                                         @Parameter(description = "专辑寻找")
                                         @RequestParam(value = "album", required = false) String album,
                                         
                                         @Parameter(description = "歌曲寻找")
                                         @RequestParam(value = "title", required = false) String title,
                                         
                                         @Parameter(description = "搜索所有字段")
                                         @RequestParam(value = "any", required = false) Boolean any,
                                         
                                         @Parameter(description = "要返回的最大结果数")
                                         @RequestParam(value = "count", required = false, defaultValue = "20") Long count,
                                         
                                         @Parameter(description = "搜索结果偏移量。用于分页")
                                         @RequestParam(value = "offset", required = false, defaultValue = "0") Long offset,
                                         
                                         @Parameter(description = "只返回比此更新的匹配项。自1970年以来以毫秒计")
                                         @RequestParam(value = "newerThan", required = false) Long newerThan
    
    ) {
        SearchRes res = searchingApi.search(req, artist, album, title, any, count, offset, newerThan);
        return res.success(req);
    }
    
    @Operation(summary = "返回符合给定搜索条件的专辑、艺术家和歌曲。支持对结果进行分页")
    @ApiResponse(responseCode = HttpStatusStr.OK,
                 content = {
                         @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Search2Res.class)),
                         @Content(mediaType = MediaType.APPLICATION_XML_VALUE, schema = @Schema(implementation = Search2Res.class))
                 }
    )
    @GetMapping({"/search2.view", "/search2"})
    @ManualSerialize
    public ResponseEntity<String> search2(SubsonicCommonReq req,
                                          
                                          @Parameter(description = "搜索查询")
                                          @RequestParam(value = "query") String query,
                                          
                                          @Parameter(description = "返回的最大艺术家数量")
                                          @RequestParam(value = "artistCount", required = false, defaultValue = "20") Long artistCount,
                                          
                                          @Parameter(description = "艺术家的搜索结果偏移量。用于分页")
                                          @RequestParam(value = "artistOffset", required = false, defaultValue = "0") Long artistOffset,
                                          
                                          @Parameter(description = "返回的最大艺术家数")
                                          @RequestParam(value = "albumCount", required = false, defaultValue = "20") Long albumCount,
                                          
                                          @Parameter(description = "相册的搜索结果偏移量。用于分页")
                                          @RequestParam(value = "albumOffset", required = false, defaultValue = "0") Long albumOffset,
                                          
                                          @Parameter(description = "返回的最大歌曲数")
                                          @RequestParam(value = "songCount", required = false, defaultValue = "20") Long songCount,
                                                  
                                                  @Parameter(description = "歌曲的搜索结果偏移量。用于分页。自1970年以来以毫秒计")
                                                  @RequestParam(value = "songOffset", required = false, defaultValue = "0") Long songOffset,
                                                  
                                                  @Parameter(description = "从1.12.0开始）仅返回具有给定ID的音乐文件夹中的结果。参见 getMusicFolders ")
                                                  @RequestParam(value = "musicFolderId", required = false) Long musicFolderId
    
    ) {
        Search2Res res = searchingApi.search2(req, query, artistCount, artistOffset, albumCount, albumOffset, songCount, songOffset, musicFolderId);
        return res.success(req);
    }
    
    @Operation(summary = "返回符合给定搜索条件的专辑、艺术家和歌曲。支持对结果进行分页")
    @ApiResponse(responseCode = HttpStatusStr.OK,
                 content = {
                         @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Search3Res.class)),
                         @Content(mediaType = MediaType.APPLICATION_XML_VALUE, schema = @Schema(implementation = Search3Res.class))
                 }
    )
    @GetMapping({"/search3.view", "/search3"})
    @ManualSerialize
    public ResponseEntity<String> search3(SubsonicCommonReq req,
                                          
                                          @Parameter(description = "搜索查询")
                                          @RequestParam(value = "query") String query,
                                          
                                          @Parameter(description = "返回的最大艺术家数量")
                                          @RequestParam(value = "artistCount", required = false, defaultValue = "20") Long artistCount,
                                          
                                          @Parameter(description = "艺术家的搜索结果偏移量。用于分页")
                                          @RequestParam(value = "artistOffset", required = false, defaultValue = "0") Long artistOffset,
                                          
                                          @Parameter(description = "返回的最大艺术家数")
                                          @RequestParam(value = "albumCount", required = false, defaultValue = "20") Long albumCount,
                                          
                                          @Parameter(description = "相册的搜索结果偏移量。用于分页")
                                          @RequestParam(value = "albumOffset", required = false, defaultValue = "0") Long albumOffset,
                                          
                                          @Parameter(description = "返回的最大歌曲数")
                                          @RequestParam(value = "songCount", required = false, defaultValue = "20") Long songCount,
                                                  
                                                  @Parameter(description = "歌曲的搜索结果偏移量。用于分页。自1970年以来以毫秒计")
                                                  @RequestParam(value = "songOffset", required = false, defaultValue = "0") Long songOffset,
                                                  
                                                  @Parameter(description = "从1.12.0开始）仅返回具有给定ID的音乐文件夹中的结果。参见 getMusicFolders ")
                                                  @RequestParam(value = "musicFolderId", required = false) Long musicFolderId
    
    ) {
        Search3Res res = searchingApi.search3(req, query, artistCount, artistOffset, albumCount, albumOffset, songCount, songOffset, musicFolderId);
        return res.success(req);
    }
}
