package com.ruby.rubia_server.core.base;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CrudMetadata {
    private String entityName;
    private List<String> requiredRelations;
    private List<String> optionalRelations;
    private List<String> customQueries;
    private boolean enableSoftDelete;
    private String defaultSortField;
    private String defaultSortDirection;
}