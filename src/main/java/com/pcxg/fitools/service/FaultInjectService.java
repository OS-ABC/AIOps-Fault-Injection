package com.pcxg.fitools.service;

import com.pcxg.fitools.entity.FaultInjectInfo;

public interface FaultInjectService {
    boolean inject(FaultInjectInfo faultInjectInfo) throws Exception;
}
