package com.ruby.rubia_server.core.base;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class DTOMapper {
    
    /**
     * Converts an entity to a DTO using reflection.
     * This is a simple implementation that can be enhanced with libraries like MapStruct.
     */
    public static <T, D> D toDTO(T entity, Class<D> dtoClass) {
        log.debug("Converting {} to {}", entity.getClass().getSimpleName(), dtoClass.getSimpleName());
        
        try {
            // For now, this is a placeholder implementation
            // In practice, you would use a mapping library like MapStruct
            // or implement specific conversion logic for each entity
            
            // This method should be overridden in each controller
            throw new UnsupportedOperationException(
                "DTOMapper.toDTO is a placeholder. Override convertToDTO method in your controller."
            );
            
        } catch (Exception e) {
            log.error("Error converting entity to DTO", e);
            throw new RuntimeException("Failed to convert entity to DTO", e);
        }
    }
}