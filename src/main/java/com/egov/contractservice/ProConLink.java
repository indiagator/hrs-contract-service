package com.egov.contractservice;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProConLink
{
    String projectid;
    List<Long> contractorids;
}
