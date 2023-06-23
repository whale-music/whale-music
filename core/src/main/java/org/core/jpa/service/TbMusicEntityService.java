package org.core.jpa.service;

import org.core.jpa.entity.TbMusicEntity;
import org.core.jpa.model.dto.TbMusicEntityDTO;
import org.core.jpa.model.vo.TbMusicEntityQueryVO;
import org.core.jpa.model.vo.TbMusicEntityUpdateVO;
import org.core.jpa.model.vo.TbMusicEntityVO;
import org.core.jpa.repository.TbMusicEntityRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class TbMusicEntityService {
    
    @Autowired
    private TbMusicEntityRepository tbMusicEntityRepository;
    
    public Long save(TbMusicEntityVO vO) {
        TbMusicEntity bean = new TbMusicEntity();
        BeanUtils.copyProperties(vO, bean);
        bean = tbMusicEntityRepository.save(bean);
        return bean.getId();
    }
    
    public void delete(Long id) {
        tbMusicEntityRepository.deleteById(id);
    }
    
    public void update(Long id, TbMusicEntityUpdateVO vO) {
        TbMusicEntity bean = requireOne(id);
        BeanUtils.copyProperties(vO, bean);
        tbMusicEntityRepository.save(bean);
    }
    
    public TbMusicEntityDTO getById(Long id) {
        TbMusicEntity original = requireOne(id);
        return toDTO(original);
    }
    
    public Page<TbMusicEntityDTO> query(TbMusicEntityQueryVO vO) {
        throw new UnsupportedOperationException();
    }
    
    private TbMusicEntityDTO toDTO(TbMusicEntity original) {
        TbMusicEntityDTO bean = new TbMusicEntityDTO();
        BeanUtils.copyProperties(original, bean);
        return bean;
    }
    
    private TbMusicEntity requireOne(Long id) {
        return tbMusicEntityRepository.findById(id)
                                      .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
    }
}
