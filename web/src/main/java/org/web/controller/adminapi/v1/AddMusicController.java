package org.web.controller.adminapi.v1;

import lombok.extern.slf4j.Slf4j;
import org.api.admin.AddMusicCompatibility;
import org.core.common.result.R;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController("AdminAddMusic")
@RequestMapping("/add")
@Slf4j
@CrossOrigin
public class AddMusicController {
    @Autowired
    private AddMusicCompatibility addMusic;
    
    
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile uploadFile) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        return R.success(addMusic.uploadMusicFile(uploadFile));
    }
}
