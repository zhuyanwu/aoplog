package com.zyw.aoplog.platform.controller;


import com.zyw.aoplog.platform.OperLog;
import com.zyw.aoplog.platform.ParamDTO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("log")
public class TestController {


    @PostMapping("test")
    @OperLog(operModel = "log",operType = "查询",operDesc = "讲述名字")
    public String testMethod(@RequestBody ParamDTO dto){
        return String.format("我的名字是%s", dto.getName());
    }
}
