package org.plugin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.compiler.CompilerUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.api.admin.service.MusicFlowApi;
import org.core.common.exception.BaseException;
import org.core.common.result.ResultCode;
import org.core.iservice.TbPluginMsgService;
import org.core.iservice.TbPluginService;
import org.core.iservice.TbPluginTaskService;
import org.core.pojo.TbPluginMsgPojo;
import org.core.pojo.TbPluginPojo;
import org.core.pojo.TbPluginTaskPojo;
import org.core.service.QukuService;
import org.core.utils.UserUtil;
import org.jetbrains.annotations.NotNull;
import org.plugin.common.CommonPlugin;
import org.plugin.common.TaskStatus;
import org.plugin.converter.PluginLabelValue;
import org.plugin.converter.PluginMsgRes;
import org.plugin.converter.PluginReq;
import org.plugin.converter.PluginRes;
import org.plugin.service.PluginService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PluginServiceImpl implements PluginService {
    @Autowired
    private MusicFlowApi musicFlowApi;
    
    @Autowired
    private TbPluginMsgService pluginMsgService;
    
    @Autowired
    private TbPluginTaskService pluginTaskService;
    
    @Autowired
    private TbPluginService pluginService;
    
    @Autowired
    private QukuService qukuService;
    
    private static CommonPlugin runCode(String script, String allClassName) {
        try {
            final ClassLoader classLoader = CompilerUtil.getCompiler(null)
                                                        // 被编译的源码字符串
                                                        .addSource(allClassName, script)
                                                        .compile();
            final Class<?> clazz = classLoader.loadClass(allClassName);
            log.info("clazz: {}", clazz);
            // 实例化对象c
            Object obj = ReflectUtil.newInstance(clazz);
            if (obj instanceof CommonPlugin) {
                return (CommonPlugin) obj;
            }
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new BaseException(e.getMessage());
        }
        log.error("全类名: {}", allClassName);
        log.error("script: {}", script);
        throw new BaseException(ResultCode.PLUGIN_CODE);
    }
    
    /**
     * 获取代码中全类名
     *
     * @param code 代码
     * @return 全类名
     */
    @NotNull
    private static String getClassName(String code) {
        List<String> packageList = ReUtil.findAll("package\\s[a-zA-Z]+[0-9a-zA-Z_]*(\\.[a-zA-Z]+[0-9a-zA-Z_]*)*\\.[a-zA-Z]+[0-9a-zA-Z_]*", code, 0);
        List<String> classList = ReUtil.findAll("[public]?class\\s(\\w+)\\b", code, 0);
        if (CollUtil.isEmpty(packageList) && packageList.size() == 1 && CollUtil.isEmpty(classList) && classList.size() == 1) {
            throw new NullPointerException();
        }
        String className = StringUtils.replace(classList.get(0), "public", "");
        className = StringUtils.replace(className, "class", "").trim();
        String packageStr = StringUtils.replace(packageList.get(0), "package ", "");
        return packageStr + "." + className;
    }
    
    @Override
    public List<PluginRes> getAllPlugin(Long userId, List<Long> pluginId) {
        LambdaQueryWrapper<TbPluginPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CollUtil.isNotEmpty(pluginId), TbPluginPojo::getId, pluginId);
        wrapper.eq(TbPluginPojo::getUserId, userId);
        List<TbPluginPojo> list = pluginService.list(wrapper);
        ArrayList<PluginRes> pluginRes = new ArrayList<>();
        for (TbPluginPojo tbPluginPojo : list) {
            PluginRes p = new PluginRes();
            BeanUtils.copyProperties(tbPluginPojo, p);
            pluginRes.add(p);
        }
        return pluginRes;
    }
    
    /**
     * 添加插件代码
     *
     * @param req 更新或添加插件代码
     */
    @Override
    public PluginRes saveOrUpdatePlugin(PluginReq req) {
        req.setUserId(req.getUserId() == null ? UserUtil.getUser().getId() : req.getUserId());
        pluginService.saveOrUpdate(req);
        TbPluginPojo byId = pluginService.getById(req.getId());
        PluginRes pluginRes = new PluginRes();
        BeanUtils.copyProperties(byId, pluginRes);
        return pluginRes;
    }
    
    /**
     * 查询插件入参
     *
     * @param pluginId 插件ID
     * @return 插件入参
     */
    @Override
    public List<PluginLabelValue> getPluginParams(Long pluginId) {
        TbPluginPojo byId = pluginService.getById(pluginId);
        if (byId == null) {
            throw new BaseException(ResultCode.PLUGIN_EXISTED);
        }
        String script = byId.getCode();
        String allClassName = getClassName(script);
        CommonPlugin func = runCode(script, allClassName);
        return func.getParams();
    }
    
    /**
     * 运行插件任务
     *
     * @param req      插件入参
     * @param pluginId 插件ID
     * @param onLine   是否在线运行
     * @param task     任务ID
     */
    @Async
    @Override
    public void execPluginTask(List<PluginLabelValue> req, Long pluginId, Boolean onLine, TbPluginTaskPojo task) {
        try {
            onLineExecPluginTask(req, pluginId, task);
            // 运行完成
            TbPluginTaskPojo entity = new TbPluginTaskPojo();
            entity.setId(task.getId());
            entity.setStatus(TaskStatus.STOP_STATUS);
            pluginTaskService.updateById(entity);
        } catch (Exception e) {
            // 运行错误
            TbPluginTaskPojo entity = new TbPluginTaskPojo();
            entity.setId(task.getId());
            entity.setStatus(TaskStatus.ERROR_STATUS);
            pluginTaskService.updateById(entity);
            PluginPackage pluginPackage = new PluginPackage(musicFlowApi,
                    pluginMsgService,
                    pluginTaskService,
                    qukuService,
                    task.getId(),
                    task.getUserId(),
                    null);
            pluginPackage.log((short) 3, task.toString(), String.valueOf(entity.getUserId()), e.getMessage());
            log.error(e.getMessage(), e);
            throw new BaseException(ResultCode.PLUGIN_CODE.getCode(), e.getMessage());
        }
    }
    
    /**
     * @param runtimeId 任务ID
     */
    @Override
    public List<PluginMsgRes> getPluginRuntimeMessages(Long runtimeId) {
        List<TbPluginMsgPojo> list = pluginMsgService.list(Wrappers.<TbPluginMsgPojo>lambdaQuery().eq(TbPluginMsgPojo::getTaskId, runtimeId));
        ArrayList<PluginMsgRes> pluginMsgRes = new ArrayList<>();
        for (TbPluginMsgPojo tbPluginMsgPojo : list) {
            PluginMsgRes pluginRes = new PluginMsgRes();
            BeanUtils.copyProperties(tbPluginMsgPojo, pluginRes);
            pluginMsgRes.add(pluginRes);
        }
        return pluginMsgRes;
    }
    
    /**
     * @param req      插件入参
     * @param pluginId 插件ID
     * @param task     任务ID
     */
    @Override
    public List<TbPluginMsgPojo> onLineExecPluginTask(List<PluginLabelValue> req, Long pluginId, TbPluginTaskPojo task) {
        TbPluginPojo byId = pluginService.getById(pluginId);
        if (byId == null) {
            throw new BaseException(ResultCode.PLUGIN_EXISTED);
        }
        CommonPlugin func = runCode(byId.getCode(), getClassName(byId.getCode()));
        PluginPackage pluginPackage = new PluginPackage(musicFlowApi,
                pluginMsgService,
                pluginTaskService,
                qukuService,
                task.getId(),
                task.getUserId(),
                null);
        func.apply(req, pluginPackage);
        TbPluginTaskPojo entity = new TbPluginTaskPojo();
        entity.setId(task.getId());
        entity.setStatus(TaskStatus.STOP_STATUS);
        pluginTaskService.updateById(entity);
        return pluginPackage.getLogs();
    }
    
    
    @Override
    public TbPluginTaskPojo getTbPluginTaskPojo(Long pluginId, Long userId) {
        TbPluginTaskPojo entity = new TbPluginTaskPojo();
        entity.setPluginId(pluginId);
        entity.setUserId(userId);
        entity.setStatus(TaskStatus.RUN_STATUS);
        pluginTaskService.save(entity);
        return entity;
    }
    
    /**
     * @param id       用户ID
     * @param taskPojo 任务运行信息
     * @return 返回运行信息
     */
    @Override
    public List<TbPluginTaskPojo> getTask(Long id, TbPluginTaskPojo taskPojo) {
        if (taskPojo != null && taskPojo.getId() != null) {
            TbPluginTaskPojo byId = pluginTaskService.getById(taskPojo.getId());
            return Collections.singletonList(byId);
        }
        if (taskPojo == null || taskPojo.getId() == null || taskPojo.getStatus() == null) {
            LambdaQueryWrapper<TbPluginTaskPojo> wrappers = Wrappers.lambdaQuery();
            wrappers.eq(TbPluginTaskPojo::getUserId, id);
            wrappers.orderByDesc(TbPluginTaskPojo::getCreateTime);
            return pluginTaskService.list(wrappers);
        }
        if (taskPojo.getUserId() != null) {
            LambdaQueryWrapper<TbPluginTaskPojo> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(TbPluginTaskPojo::getUserId, taskPojo.getUserId());
            wrapper.eq(taskPojo.getStatus() != null, TbPluginTaskPojo::getStatus, taskPojo.getStatus());
            wrapper.eq(taskPojo.getPluginId() != null, TbPluginTaskPojo::getPluginId, taskPojo.getPluginId());
            return pluginTaskService.list(wrapper);
        }
        return Collections.emptyList();
    }
    
    
    /**
     * 删除插件任务
     * 注意： 目前只能删除已完成或错误停止的任务
     *
     * @param id 任务ID
     */
    @Override
    public void deleteTask(Long id) {
        boolean b = pluginTaskService.removeById(id);
        if (!b) {
            throw new BaseException(ResultCode.PLUGIN_DELETE_TASK_ERROR);
        }
        LambdaQueryWrapper<TbPluginMsgPojo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(TbPluginMsgPojo::getTaskId, id);
        b = pluginMsgService.remove(queryWrapper);
        if (!b) {
            throw new BaseException(ResultCode.PLUGIN_DELETE_TASK_ERROR);
        }
    }
    
    /**
     * 删除插件
     *
     * @param id 插件ID
     */
    @Override
    public void deletePlugin(Long id) {
        boolean b = pluginService.removeById(id);
        if (!b) {
            throw new BaseException(ResultCode.PLUGIN_DELETE_ERROR);
        }
    }
}
