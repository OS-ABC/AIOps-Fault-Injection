package com.pcxg.fitools.entity.mysql;

import lombok.Data;

@Data
public class Event {
    private static final long serialVersionUID = -6167490617374518010L;
    private Integer id;
    private Integer clock;
    private Integer value;
    private String name;
    private Integer severity;

    public Event(Integer id, Integer clock, Integer value, String name, Integer severity) {
        this.id = id;
        this.clock = clock;
        this.value = value;
        this.name = name;
        this.severity = severity;
    }
}
