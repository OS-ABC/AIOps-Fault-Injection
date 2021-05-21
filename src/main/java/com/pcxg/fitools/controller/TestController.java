package com.pcxg.fitools.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.entity.SSHConf;
import com.pcxg.fitools.env.Environment;
import com.pcxg.fitools.env.HadoopDockerEnv;
import com.pcxg.fitools.service.FaultInjectService;
import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.utils.HadoopInfo;
import com.pcxg.fitools.utils.SSHConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController
public class TestController {
    @Autowired
    FaultInjectService faultInjectService;



    @RequestMapping("/test")
    public String testRun(@RequestBody @Valid FaultInjectInfo faultInjectInfo, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            //return "Invalid params";
            return Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        }
        System.out.println("get a request at " + faultInjectInfo.getEnvStartTime());
//        if (faultInjectInfo.getEnvironment() == null) {
//            faultInjectInfo.setEnvironment(new HadoopDockerEnv());
//        }
        try {
            if (!faultInjectService.inject(faultInjectInfo)) {
                return "Invalid request";
            }
        }catch (Exception e) {
            System.out.println(e);
            return "Fail";
        }

        return "Success";
    }

   
}
