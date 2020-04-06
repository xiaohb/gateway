package com.platform.gateway.client.dto;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 路由过滤器模型
 */
@EqualsAndHashCode
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayFilterDefinitionDTO {
    private String name;
    private Map<String, String> args = new LinkedHashMap<>();
}
