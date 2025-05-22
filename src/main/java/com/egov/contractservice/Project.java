package com.egov.contractservice;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "projects")
@Getter @Setter
public class Project
{
    @Id
    private String projectid;
    private Long customerid;
    private String projecttype;
    private String description;
    private Integer budget;
    private String location;
    private String status;
}
