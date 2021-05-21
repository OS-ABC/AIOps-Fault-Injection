package com.pcxg.fitools.entity.mysql;

import lombok.Data;

import java.io.Serializable;
@Data
public class Item implements Serializable {
    private static final long serialVersionUID = -9157547710719313052L;
    private Integer id;
    private String name;
    private Integer type;
    private Integer valueType;
    private String delay;
    private String units;
    private String hostName;
    private String applicationName;
    private String allClock;
    private String allValue;
}
