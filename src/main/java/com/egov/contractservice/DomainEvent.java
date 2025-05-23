package com.egov.contractservice;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DomainEvent implements Serializable
{
    String eventType;
    String dbname;
    String collectionname;
    String documentid;
    Long principalid;
    String endpoint;
    List<String> traceparent = new ArrayList<>();
}
